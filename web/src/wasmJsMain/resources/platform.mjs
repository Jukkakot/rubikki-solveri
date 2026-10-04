// Every browser call the app makes lives here (no inline js() in Kotlin: the minified build broke
// on it). Kotlin imports these through @JsModule("./platform.mjs"); only strings, numbers,
// booleans and JS objects cross the boundary.

import { zipSync } from 'fflate';

// --- Storage -------------------------------------------------------------------------------------

export function storageGet(key) {
  return localStorage.getItem(key);
}

export function storageSet(key, value) {
  localStorage.setItem(key, value);
}

export function storageRemove(key) {
  localStorage.removeItem(key);
}

export function persistStorage() {
  try {
    if (navigator.storage && navigator.storage.persist) navigator.storage.persist();
  } catch (e) { /* not available: data may be evicted under storage pressure */ }
}

// --- Camera --------------------------------------------------------------------------------------
// One stream shared by the permission gate and the preview (reference counted). Each grabbed frame
// is drawn twice: the visible part scaled down for the preview (canvas A) and the grid's square at
// a fixed size for the colour reading (canvas B). Kotlin passes the visible part (cover crop).

const cam = { stream: null, starting: null, users: 0, video: null, lastTime: -1, fresh: false };
const canvasA = document.createElement('canvas');
const canvasB = document.createElement('canvas');
const ctxA = canvasA.getContext('2d', { willReadFrequently: true });
const ctxB = canvasB.getContext('2d', { willReadFrequently: true });

function camTrack() {
  return cam.stream ? cam.stream.getVideoTracks()[0] : null;
}

function camStop() {
  if (cam.stream) cam.stream.getTracks().forEach((t) => t.stop());
  if (cam.video) cam.video.srcObject = null;
  cam.stream = null;
  cam.starting = null;
  cam.lastTime = -1;
}

function watchFrames(video) {
  // A new video frame marks the next grab as fresh; without requestVideoFrameCallback the
  // video's time is compared instead.
  if (!video.requestVideoFrameCallback) return;
  const tick = () => {
    if (cam.video !== video) return;
    cam.fresh = true;
    video.requestVideoFrameCallback(tick);
  };
  video.requestVideoFrameCallback(tick);
}

/** Starts the back camera (or any) for one more user; [done] gets "ok" or the error's name. */
export function cameraAcquire(done) {
  cam.users++;
  if (!cam.starting) {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      cam.users--;
      done('NotSupportedError');
      return;
    }
    cam.starting = navigator.mediaDevices.getUserMedia({
      video: { facingMode: { ideal: 'environment' }, width: { ideal: 1280 }, height: { ideal: 720 } },
      audio: false,
    }).then((stream) => {
      cam.stream = stream;
      if (!cam.video) {
        // Off screen but rendered (display:none stops frames in Safari).
        const v = document.createElement('video');
        v.muted = true;
        v.playsInline = true;
        v.setAttribute('playsinline', '');
        v.style.cssText = 'position:fixed;left:0;top:0;width:1px;height:1px;opacity:0;pointer-events:none';
        document.body.appendChild(v);
        cam.video = v;
      }
      cam.video.srcObject = stream;
      watchFrames(cam.video);
      return cam.video.play();
    });
  }
  cam.starting.then(
    () => {
      if (cam.users <= 0) camStop();
      done('ok');
    },
    (e) => {
      cam.users = Math.max(0, cam.users - 1);
      cam.starting = null;
      done((e && e.name) || 'Error');
    },
  );
}

/** One user less; the stream stops when nobody uses it. */
export function cameraRelease() {
  cam.users = Math.max(0, cam.users - 1);
  if (cam.users === 0 && cam.stream) camStop();
}

export function cameraVideoWidth() {
  return cam.stream && cam.video && cam.video.readyState >= 2 ? cam.video.videoWidth : 0;
}

export function cameraVideoHeight() {
  return cam.stream && cam.video && cam.video.readyState >= 2 ? cam.video.videoHeight : 0;
}

/**
 * Draws the newest frame if there is one: the visible part [x,y,w,h] scaled so its long side is
 * [previewLong] into canvas A, the centred square of its shorter side into [size]² canvas B.
 * Returns false when no new frame has arrived since the last grab.
 */
export function cameraGrab(x, y, w, h, previewLong, size) {
  const v = cam.video;
  if (!cam.stream || !v || v.readyState < 2) return false;
  if (v.requestVideoFrameCallback) {
    if (!cam.fresh) return false;
    cam.fresh = false;
  } else {
    if (v.currentTime === cam.lastTime) return false;
    cam.lastTime = v.currentTime;
  }
  const scale = previewLong / Math.max(w, h);
  const pw = Math.max(1, Math.round(w * scale));
  const ph = Math.max(1, Math.round(h * scale));
  if (canvasA.width !== pw || canvasA.height !== ph) { canvasA.width = pw; canvasA.height = ph; }
  ctxA.drawImage(v, x, y, w, h, 0, 0, pw, ph);
  const side = Math.min(w, h);
  if (canvasB.width !== size) { canvasB.width = size; canvasB.height = size; }
  ctxB.drawImage(v, x + (w - side) / 2, y + (h - side) / 2, side, side, 0, 0, size, size);
  return true;
}

export function cameraPreviewWidth() { return canvasA.width; }
export function cameraPreviewHeight() { return canvasA.height; }

/** Canvas A's RGBA bytes. */
export function cameraPreviewData() {
  return new Int8Array(ctxA.getImageData(0, 0, canvasA.width, canvasA.height).data.buffer);
}

/** Canvas B's RGBA bytes (the grid square). */
export function cameraAnalysisData() {
  return new Int8Array(ctxB.getImageData(0, 0, canvasB.width, canvasB.height).data.buffer);
}

function capabilities() {
  const t = camTrack();
  try { return (t && t.getCapabilities && t.getCapabilities()) || {}; } catch (e) { return {}; }
}

export function cameraTorchSupported() {
  return !!capabilities().torch;
}

export function cameraSetTorch(on) {
  const t = camTrack();
  if (t && capabilities().torch) t.applyConstraints({ advanced: [{ torch: !!on }] }).catch(() => {});
}

/** Holds exposure and white balance where the camera can: "true", "false" or "unsupported". */
export function cameraLockExposure(lock) {
  const t = camTrack();
  const caps = capabilities();
  const can = (k) => Array.isArray(caps[k]) && caps[k].includes('manual') && caps[k].includes('continuous');
  if (!t || !can('exposureMode') || !can('whiteBalanceMode')) return 'unsupported';
  const mode = lock ? 'manual' : 'continuous';
  t.applyConstraints({ advanced: [{ exposureMode: mode, whiteBalanceMode: mode }] }).catch(() => {});
  return String(!!lock);
}

// --- Page ----------------------------------------------------------------------------------------

export function log(level, line) {
  if (level === 'ERROR') console.error(line);
  else if (level === 'WARN') console.warn(line);
  else console.info(line);
}

export function hideLoading() {
  const el = document.getElementById('loading');
  if (el) el.remove();
}

export function reload() {
  location.reload();
}

export function queryFlag(name) {
  return new URLSearchParams(location.search).has(name);
}

export function setThemeColor(color) {
  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) meta.setAttribute('content', color);
}

export function reducedMotion() {
  return !!(window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches);
}

export function vibrate(ms) {
  try {
    if (navigator.vibrate) navigator.vibrate(ms);
  } catch (e) { /* not allowed or not supported: silent */ }
}

export function formatDateTime(epochMillis, language, timeOnly) {
  const options = timeOnly ? { timeStyle: 'medium' } : { dateStyle: 'short', timeStyle: 'short' };
  try {
    return new Intl.DateTimeFormat(language, options).format(new Date(epochMillis));
  } catch (e) {
    return new Date(epochMillis).toISOString();
  }
}

/**
 * Shares the log text and the PNG pictures (names and base64 data, one per line) through the share
 * sheet when the browser can share files. When it cannot, or refuses, one zip with the log and the
 * pictures is downloaded instead (a refused share uses up the tap, so a second share cannot work).
 * [report]
 * gets the outcome (shared, downloaded, cancelled) and the refusal ("" when none).
 */
export function shareOrDownload(logName, logText, pictureNames, pictureData, report) {
  const logBytes = new TextEncoder().encode(logText);
  const names = pictureNames ? pictureNames.split('\n') : [];
  const data = pictureData ? pictureData.split('\n') : [];
  const pictures = names.map((name, i) => [name, Uint8Array.from(atob(data[i]), (c) => c.charCodeAt(0))]);
  // Chromium shares at most 10 files at once (more is refused, which also uses up the tap): the log
  // and the newest 9 pictures (oldest first in the list). The zip fallback has them all.
  const files = [new File([logBytes], logName, { type: 'text/plain' })]
    .concat(pictures.slice(-9).map(([name, bytes]) => new File([bytes], name, { type: 'image/png' })));
  const fallback = (error) => {
    const entries = { [logName]: [logBytes, { level: 0 }] };
    pictures.forEach(([name, bytes]) => { entries[name] = [bytes, { level: 0 }]; });
    const zipName = logName.replace(/\.txt$/, '') + '.zip';
    download(new Blob([zipSync(entries)], { type: 'application/zip' }), zipName);
    report('downloaded', error);
  };
  let canShare = false;
  try {
    canShare = !!(navigator.canShare && navigator.share && navigator.canShare({ files }));
  } catch (e) {
    canShare = false;
  }
  if (!canShare) {
    fallback('cannot share files');
    return;
  }
  navigator.share({ files, title: logName })
    .then(() => report('shared', ''))
    .catch((e) => {
      if (e && e.name === 'AbortError') report('cancelled', '');
      else fallback((e && (e.name + ': ' + e.message)) || String(e));
    });
}

function download(blob, name) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = name;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}

/** Uncaught errors and rejected promises: [report] gets "kind: message\nstack". */
export function installCrashHooks(report) {
  window.addEventListener('error', (e) => {
    report('error: ' + (e.message || '') + '\n' + ((e.error && e.error.stack) || ''));
  });
  window.addEventListener('unhandledrejection', (e) => {
    const r = e.reason;
    report('unhandledrejection: ' + ((r && r.message) || String(r)) + '\n' + ((r && r.stack) || ''));
  });
}

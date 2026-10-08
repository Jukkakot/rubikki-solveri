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

const cam = { stream: null, starting: null, users: 0, video: null, lastTime: -1, fresh: false, info: '' };
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

// Phones have several back cameras and the browser's pick may have no torch, so the first time
// the other back cameras are tried and the first with a torch is kept. The camera found is
// remembered, so later scans open it directly without trying the others again.

const CAMERA_KEY = 'camera.device';
const FRONT = /front|user|selfie|etu/i;

function openCamera(choice) {
  return navigator.mediaDevices.getUserMedia({
    video: { ...choice, width: { ideal: 1280 }, height: { ideal: 720 } },
    audio: false,
  });
}

function stopStream(stream) {
  stream.getTracks().forEach((t) => t.stop());
}

function trackOf(stream) {
  return stream.getVideoTracks()[0];
}

function hasTorch(stream) {
  const t = trackOf(stream);
  try { return !!(t && t.getCapabilities && t.getCapabilities().torch); } catch (e) { return false; }
}

/** Some browsers report the torch a moment after the camera opens. */
async function torchOf(stream) {
  if (hasTorch(stream)) return true;
  await new Promise((r) => setTimeout(r, 300));
  return hasTorch(stream);
}

function deviceOf(stream) {
  const t = trackOf(stream);
  try { return (t && t.getSettings && t.getSettings().deviceId) || ''; } catch (e) { return ''; }
}

function facesUser(stream) {
  const t = trackOf(stream);
  try { return (t && t.getSettings && t.getSettings().facingMode) === 'user'; } catch (e) { return false; }
}

function remember(stream, tried) {
  const t = trackOf(stream);
  cam.info = `${(t && t.label) || '?'}; torch=${hasTorch(stream)}; tried=${tried}`;
  try { localStorage.setItem(CAMERA_KEY, deviceOf(stream)); } catch (e) { /* not remembered */ }
}

async function openBackCamera() {
  let saved = null;
  try { saved = localStorage.getItem(CAMERA_KEY); } catch (e) { /* none */ }
  if (saved) {
    try {
      const stream = await openCamera({ deviceId: { exact: saved } });
      const t = trackOf(stream);
      cam.info = `${(t && t.label) || '?'}; torch=${await torchOf(stream)}; remembered`;
      return stream;
    } catch (e) {
      try { localStorage.removeItem(CAMERA_KEY); } catch (e2) { /* ignore */ }
    }
  }
  let stream = await openCamera({ facingMode: { ideal: 'environment' } });
  if (await torchOf(stream) || !navigator.mediaDevices.enumerateDevices) {
    remember(stream, 1);
    return stream;
  }
  const first = deviceOf(stream);
  let others = [];
  try {
    others = (await navigator.mediaDevices.enumerateDevices())
      .filter((d) => d.kind === 'videoinput' && d.deviceId && d.deviceId !== first && !FRONT.test(d.label));
  } catch (e) { /* only the first camera */ }
  if (!first || others.length === 0) {
    remember(stream, 1);
    return stream;
  }
  // Phones open one camera at a time: close the first while trying the others.
  stopStream(stream);
  let tried = 1;
  for (const d of others) {
    let other;
    try { other = await openCamera({ deviceId: { exact: d.deviceId } }); } catch (e) { continue; }
    tried++;
    if (!facesUser(other) && await torchOf(other)) {
      remember(other, tried);
      return other;
    }
    stopStream(other);
  }
  stream = await openCamera({ deviceId: { exact: first } });
  remember(stream, tried);
  return stream;
}

/** The camera in use, for the log: its name, whether it has a torch and how it was chosen. */
export function cameraInfo() {
  return cam.info || '';
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
    cam.starting = openBackCamera().then((stream) => {
      cam.stream = stream;
      if (!cam.video) {
        // Off screen but rendered (display:none stops frames in Safari).
        const v = document.createElement('video');
        v.muted = true;
        v.playsInline = true;
        v.setAttribute('playsinline', '');
        v.style.cssText = HIDDEN;
        document.body.insertBefore(v, document.body.firstChild);
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

// The camera's picture as the user sees it: the video element itself, under the app's canvas, where
// the preview box is (the box is drawn transparent). Shown at the camera's own resolution and frame
// rate; the scaled copies above are only for reading.

const HIDDEN = 'position:fixed;left:0;top:0;width:1px;height:1px;opacity:0;pointer-events:none;z-index:0';

/** Shows the camera's video at [x, y] (CSS pixels from the page's top left), [w]×[h], cropped to cover. */
export function cameraShow(x, y, w, h) {
  shown.box = { x, y, w, h };
  placePicture();
}

/** Hides the video again (it keeps playing for the reading). */
export function cameraHide() {
  shown.box = null;
  shown.on = false;
  placePicture();
}

// The read picture (`scan-feedback` design 8): with the worker, each picture sent also gets a copy at
// the box's size, kept under its number. When the worker's faces for it come back and Kotlin draws
// their marks, cameraShowFrame puts that copy on a canvas in the video's place: the marks then lie on
// the very picture they were read from (the picture is the scan's rate and ~60 ms late). Without the
// worker or createImageBitmap the live video stays.

const shown = { box: null, on: false, canvas: null, ctx: null, seq: 0, frames: new Map(), times: new Map() };
const SHOW_LONG_MAX = 720; // `scan-paint-steady`: at the full pixel ratio the copy took 20–27 ms a picture on the phone

/** Puts the video, or the read picture's canvas while it is on, where the box is; hides the other. */
function placePicture() {
  const b = shown.box;
  const at = b ? `position:fixed;left:${b.x}px;top:${b.y}px;width:${b.w}px;height:${b.h}px;pointer-events:none;z-index:0` : null;
  if (cam.video) cam.video.style.cssText = at && !shown.on ? `${at};object-fit:cover` : HIDDEN;
  if (shown.canvas) shown.canvas.style.cssText = at && shown.on ? at : HIDDEN;
}

/** The read picture's copy size for a crop of [w]×[h]: the box's size in device pixels, its long side at most SHOW_LONG_MAX. */
function showSize(w, h) {
  const b = shown.box;
  if (!b) return null;
  const dpr = window.devicePixelRatio || 1;
  const k = Math.min(dpr, SHOW_LONG_MAX / Math.max(b.w, b.h));
  return [Math.max(1, Math.round(b.w * k)), Math.max(1, Math.round(b.h * k))];
}

function dropFrame(seq) {
  const f = shown.frames.get(seq);
  if (f) f.close();
  shown.frames.delete(seq);
  shown.times.delete(seq);
}

/** Draws picture [seq]'s copy in the video's place (older copies are closed); false when there is none. */
export function cameraShowFrame(seq) {
  const f = shown.frames.get(seq);
  if (!f) return false;
  if (!shown.canvas) {
    shown.canvas = document.createElement('canvas');
    shown.ctx = shown.canvas.getContext('2d');
    document.body.insertBefore(shown.canvas, document.body.firstChild);
  }
  if (shown.canvas.width !== f.width || shown.canvas.height !== f.height) {
    shown.canvas.width = f.width;
    shown.canvas.height = f.height;
  }
  shown.ctx.drawImage(f, 0, 0);
  for (const k of [...shown.frames.keys()]) if (k <= seq) dropFrame(k);
  if (!shown.on) {
    shown.on = true;
    placePicture();
  }
  return true;
}

/** Back to the live video (the scan stopped or the worker is not used). */
function showLive() {
  for (const k of [...shown.frames.keys()]) dropFrame(k);
  if (shown.on) {
    shown.on = false;
    placePicture();
  }
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

// The video scan sets the camera for the cube (`camera-exposure` design 4): where it measures light
// and focuses, and how far below its own exposure. Each only where the browser lists it; the rest is
// skipped and named in the `scan.camera` line.

function supported(name) {
  try { return !!(navigator.mediaDevices.getSupportedConstraints() || {})[name]; } catch (e) { return false; }
}

function compensation() {
  const c = capabilities().exposureCompensation;
  return c && typeof c.min === 'number' && typeof c.step === 'number' && c.step > 0 && c.min < 0 ? c : null;
}

/** The lowest exposure compensation (EV, 0 when it cannot be set). */
export function cameraCompensationMin() {
  const c = compensation();
  return c ? c.min : 0;
}

/** The exposure compensation's step (EV, 0 when it cannot be set). */
export function cameraCompensationStep() {
  const c = compensation();
  return c ? c.step : 0;
}

/** Sets the exposure compensation to [ev] where the camera can. */
export function cameraSetCompensation(ev) {
  const t = camTrack();
  if (t && compensation()) t.applyConstraints({ advanced: [{ exposureCompensation: ev }] }).catch(() => {});
}

function focusModes() {
  const m = capabilities().focusMode;
  return Array.isArray(m) ? m : [];
}

/**
 * Measures light and focuses at ([x], [y]) (shares of the whole video frame) where the browser lets
 * the page; a negative [x] goes back to the whole picture. Continuous focus where offered, else one
 * focus at the point.
 */
export function cameraPointOfInterest(x, y) {
  const t = camTrack();
  if (!t) return;
  const c = {};
  if (supported('pointsOfInterest')) c.pointsOfInterest = x < 0 ? [] : [{ x, y }];
  const modes = focusModes();
  if (x >= 0 && modes.includes('continuous')) c.focusMode = 'continuous';
  else if (x >= 0 && modes.includes('single-shot')) c.focusMode = 'single-shot';
  if (Object.keys(c).length > 0) t.applyConstraints({ advanced: [c] }).catch(() => {});
}

/** What the camera lets the page do, for the log: focus modes, compensation, point, torch, lock. */
export function cameraAbilities() {
  const caps = capabilities();
  const c = compensation();
  const modes = focusModes();
  const lock = Array.isArray(caps.exposureMode) && caps.exposureMode.includes('manual');
  return `focus=${modes.length ? modes.join(',') : 'none'}; compensation=${c ? `${c.min}..${c.max} step ${c.step}` : 'none'}; ` +
    `point=${supported('pointsOfInterest')}; torch=${!!caps.torch}; lock=${lock}`;
}

// The video scan's faces are found in a Web Worker (`scan-worker.js`, the webworker module), so the
// page's one thread only draws (`camera-exposure` design 8). One picture at a time is in the worker;
// a newer one waits in place of an older (only the newest is kept). Kotlin falls back to reading on
// the page when the worker fails.

const scan = { worker: null, ready: false, busy: false, pending: null, onFaces: null, onFail: null, timer: 0, inFlight: 0 };
const WORKER_START_MS = 15000;

function workerFail(reason) {
  const onFail = scan.onFail;
  scanWorkerStop();
  if (onFail) onFail(reason);
}

function workerPost(msg) {
  scan.busy = true;
  scan.inFlight = msg.seq || 0;
  const { seq, ...body } = msg;
  scan.worker.postMessage(body, msg.bitmap ? [msg.bitmap] : [msg.data]);
}

/**
 * Starts the worker; [onFaces] gets each picture's faces as numbers, its number (0: no read picture
 * kept) and the ms its read picture's copy took, [onFail] the reason it cannot be used.
 */
export function scanWorkerStart(onFaces, onFail) {
  scanWorkerStop();
  scan.onFaces = onFaces;
  scan.onFail = onFail;
  if (typeof Worker === 'undefined' || typeof OffscreenCanvas === 'undefined') {
    workerFail('not supported');
    return;
  }
  let w;
  try {
    w = new Worker('scan-worker.js');
  } catch (e) {
    workerFail((e && e.name) || 'Error');
    return;
  }
  scan.worker = w;
  scan.timer = setTimeout(() => { if (scan.worker === w && !scan.ready) workerFail('no answer'); }, WORKER_START_MS);
  w.onerror = (e) => { if (scan.worker === w) workerFail('error: ' + ((e && e.message) || 'load failed')); };
  w.onmessage = (e) => {
    if (scan.worker !== w) return;
    const m = e.data || {};
    if (m.ready) {
      scan.ready = true;
      clearTimeout(scan.timer);
      return;
    }
    if (m.error) {
      workerFail('error: ' + m.error);
      return;
    }
    scan.busy = false;
    const answered = scan.inFlight;
    if (scan.pending) {
      const next = scan.pending;
      scan.pending = null;
      workerPost(next);
    }
    // Copies of earlier answers never shown go: the newest answer is the one to show.
    for (const k of [...shown.frames.keys()]) if (k < answered) dropFrame(k);
    const seq = shown.frames.has(answered) ? answered : 0;
    if (scan.onFaces) scan.onFaces(m.faces, seq, shown.times.get(seq) || 0);
  };
}

/** Whether pictures can go to the worker. */
export function scanWorkerReady() {
  return !!(scan.worker && scan.ready);
}

/** Whether the worker is ready and has nothing to read: the next picture goes to it at once. */
export function scanWorkerIdle() {
  return scanWorkerReady() && !scan.busy && !scan.pending;
}

function workerQueue(msg) {
  if (!scan.worker) {
    if (msg.bitmap) msg.bitmap.close();
    if (msg.seq) dropFrame(msg.seq);
    return;
  }
  if (!scan.busy) {
    workerPost(msg);
    return;
  }
  if (scan.pending) {
    if (scan.pending.bitmap) scan.pending.bitmap.close();
    if (scan.pending.seq) dropFrame(scan.pending.seq);
  }
  scan.pending = msg;
}

/**
 * Sends the newest frame's visible part [x,y,w,h] to the worker scaled so its long side is
 * [previewLong]: as an ImageBitmap made by the browser, or where it cannot make one, as canvas A's
 * pixels (drawn by the last cameraGrab).
 */
export function scanWorkerSend(x, y, w, h, previewLong) {
  const v = cam.video;
  if (!scanWorkerReady() || !v) return;
  const scale = previewLong / Math.max(w, h);
  const pw = Math.max(1, Math.round(w * scale));
  const ph = Math.max(1, Math.round(h * scale));
  if (typeof createImageBitmap === 'function') {
    // The read picture's copy of the same video frame, made in the same task (`scan-feedback`).
    const size = showSize(w, h);
    const seq = ++shown.seq;
    const started = performance.now();
    const copy = size
      ? createImageBitmap(v, x, y, w, h, { resizeWidth: size[0], resizeHeight: size[1], resizeQuality: 'medium' })
        .then((b) => { shown.times.set(seq, performance.now() - started); return b; }, () => null)
      : Promise.resolve(null);
    const sent = createImageBitmap(v, x, y, w, h, { resizeWidth: pw, resizeHeight: ph, resizeQuality: 'low' });
    Promise.all([sent, copy])
      .then(([bitmap, display]) => {
        if (display) {
          if (scan.worker) shown.frames.set(seq, display); else display.close();
        }
        workerQueue({ bitmap, seq: display ? seq : 0 });
      })
      .catch(() => { copy.then((d) => d && d.close()); shown.times.delete(seq); sendCanvas(); });
  } else {
    sendCanvas();
  }
}

function sendCanvas() {
  const data = ctxA.getImageData(0, 0, canvasA.width, canvasA.height).data;
  workerQueue({ width: canvasA.width, height: canvasA.height, data: data.buffer });
}

export function scanWorkerStop() {
  clearTimeout(scan.timer);
  if (scan.worker) scan.worker.terminate();
  if (scan.pending && scan.pending.bitmap) scan.pending.bitmap.close();
  showLive();
  scan.worker = null;
  scan.ready = false;
  scan.busy = false;
  scan.pending = null;
  scan.onFaces = null;
  scan.onFail = null;
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

// A screen wake lock while wanted; the browser drops it when the page is hidden, so it is taken
// again when the page shows.
let wakeWanted = false;
let wakeLock = null;

async function takeWakeLock() {
  try {
    if (wakeWanted && !wakeLock && navigator.wakeLock && document.visibilityState === 'visible') {
      wakeLock = await navigator.wakeLock.request('screen');
      wakeLock.addEventListener('release', () => { wakeLock = null; });
      if (!wakeWanted) { wakeLock.release(); wakeLock = null; }
    }
  } catch (e) { /* not allowed or not supported: silent */ }
}

document.addEventListener('visibilitychange', takeWakeLock);

export function keepScreenOn(on) {
  wakeWanted = on;
  if (on) takeWakeLock();
  else if (wakeLock) { wakeLock.release().catch(() => {}); wakeLock = null; }
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

// --- Lost graphics -------------------------------------------------------------------------------
// Phone browsers drop a background tab's WebGL context, and the drawing engine cannot recover: its
// next frame fails. The page reloads itself instead once it is visible (the route is in the URL).
// The canvas sits in Compose's shadow root and the event does not leave it, so every canvas that
// gets a WebGL context is watched (getContext is wrapped before Compose creates its canvas).

const GRAPHICS_RELOAD_KEY = 'rubikki.graphicsReloadAt';
const GRAPHICS_RELOAD_GAP_MS = 30000;

/** [report] gets true when the page will reload, false when it reloaded too recently to try again. */
export function installGraphicsLostHook(report) {
  let pending = false;
  const onLost = () => {
    if (pending) return;
    let last = 0;
    try { last = Number(sessionStorage.getItem(GRAPHICS_RELOAD_KEY)) || 0; } catch (e) { /* blocked */ }
    if (Date.now() - last < GRAPHICS_RELOAD_GAP_MS) {
      report(false);
      return;
    }
    pending = true;
    try { sessionStorage.setItem(GRAPHICS_RELOAD_KEY, String(Date.now())); } catch (e) { /* blocked */ }
    report(true);
    if (!document.hidden) location.reload();
    else document.addEventListener('visibilitychange', () => { if (!document.hidden) location.reload(); });
  };
  const watched = new WeakSet();
  const getContext = HTMLCanvasElement.prototype.getContext;
  HTMLCanvasElement.prototype.getContext = function (type, ...rest) {
    const ctx = getContext.call(this, type, ...rest);
    if (ctx && String(type).startsWith('webgl') && !watched.has(this)) {
      watched.add(this);
      this.addEventListener('webglcontextlost', onLost);
    }
    return ctx;
  };
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

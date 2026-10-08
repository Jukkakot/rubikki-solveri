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

// The read picture (`scan-feedback` design 8): with the worker, each picture sent is also drawn, at the
// box's size, onto one of a few canvases kept where the box is, under its number. When the worker's
// faces for it come back and Kotlin draws their marks, cameraShowFrame makes that canvas the visible
// one in the video's place: the marks then lie on the very picture they were read from (the picture is
// the scan's rate and ~60 ms late). The copy is one drawImage when the picture is sent
// (`scan-read-picture-android`: createImageBitmap with a resize took 15–22 ms a picture on the phone).
// Without the worker the live video stays.

const shown = { box: null, on: false, pool: [], seq: 0, frames: new Map(), times: new Map() };
const SHOW_LONG_MAX = 720; // `scan-paint-steady`: at the full pixel ratio the copy took 20–27 ms a picture on the phone
const POOL_SIZE = 5; // shown; in the finder and waiting for it; in the scan worker and waiting for it (`scan-speed-up-4`)

/** Puts the video, or the shown read picture's canvas while one is on, where the box is; hides the rest. */
function placePicture() {
  const b = shown.box;
  const at = b ? `position:fixed;left:${b.x}px;top:${b.y}px;width:${b.w}px;height:${b.h}px;pointer-events:none;z-index:0` : null;
  if (cam.video) cam.video.style.cssText = at && !shown.on ? `${at};object-fit:cover` : HIDDEN;
  for (const p of shown.pool) p.canvas.style.cssText = at && shown.on && p.state === 'shown' ? at : HIDDEN;
}

/** The read picture's copy size for a crop of [w]×[h]: the box's size in device pixels, its long side at most SHOW_LONG_MAX. */
function showSize() {
  const b = shown.box;
  if (!b) return null;
  const dpr = window.devicePixelRatio || 1;
  const k = Math.min(dpr, SHOW_LONG_MAX / Math.max(b.w, b.h));
  return [Math.max(1, Math.round(b.w * k)), Math.max(1, Math.round(b.h * k))];
}

/** A canvas of the pool that is neither shown nor waiting for its faces, made when the pool is not full; else null. */
function freeCanvas() {
  const free = shown.pool.find((p) => p.state === 'free');
  if (free) return free;
  if (shown.pool.length >= POOL_SIZE) return null;
  const canvas = document.createElement('canvas');
  canvas.style.cssText = HIDDEN;
  document.body.insertBefore(canvas, document.body.firstChild);
  const p = { canvas, ctx: canvas.getContext('2d'), state: 'free' };
  shown.pool.push(p);
  return p;
}

/** Draws the video's [x,y,w,h] onto a free canvas as picture [seq]'s copy; false when none is free. */
function copyFrame(v, x, y, w, h, seq) {
  const size = showSize();
  const p = size && freeCanvas();
  if (!p) return false;
  const started = performance.now();
  if (p.canvas.width !== size[0] || p.canvas.height !== size[1]) {
    p.canvas.width = size[0];
    p.canvas.height = size[1];
  }
  p.ctx.drawImage(v, x, y, w, h, 0, 0, size[0], size[1]);
  p.state = 'waiting';
  shown.frames.set(seq, p);
  shown.times.set(seq, performance.now() - started);
  return true;
}

/** Picture [seq]'s copy is not needed: its canvas is free again (unless it is the one shown). */
function dropFrame(seq) {
  const p = shown.frames.get(seq);
  if (p && p.state === 'waiting') p.state = 'free';
  shown.frames.delete(seq);
  shown.times.delete(seq);
}

/** Shows picture [seq]'s copy in the video's place (older copies freed); false when there is none. */
export function cameraShowFrame(seq) {
  const p = shown.frames.get(seq);
  if (!p) return false;
  for (const q of shown.pool) if (q.state === 'shown') q.state = 'free';
  p.state = 'shown';
  for (const k of [...shown.frames.keys()]) if (k <= seq) dropFrame(k);
  shown.on = true;
  placePicture();
  return true;
}

/** Back to the live video (the scan stopped or the worker is not used). */
function showLive() {
  for (const k of [...shown.frames.keys()]) dropFrame(k);
  for (const p of shown.pool) p.state = 'free';
  shown.on = false;
  placePicture();
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
//
// Pipeline (`scan-speed-up-4`): a second worker of the same script (the scan worker, `pipe`) runs the video
// scan, the first one then only finds faces (`role:find`). The finder's faces go on to the scan worker with
// the picture's number and the time they were found; again only the newest waits. A picture is answered to
// Kotlin when its scan comes back, so its marks still lie on it. The finder reads the next picture while the
// scan works: the pictures a second follow the slower of the two, not both together. Until the scan worker
// is ready, or if it cannot start, the finder scans as before.

const scan = {
  worker: null, ready: false, busy: false, pending: null, onFaces: null, onFail: null, timer: 0, inFlight: 0, replies: new Map(), nextReply: 1,
  pipe: null, pipeline: false, lastReset: { count: 0, engine: 'RULES' },
};
const WORKER_START_MS = 15000;

function workerFail(reason) {
  const onFail = scan.onFail;
  scanWorkerStop();
  if (onFail) onFail(reason);
}

/** Starts the scan worker beside the finder; it takes over the scan once both are ready ([pipeOn]). */
function pipeStart() {
  let w;
  try {
    w = new Worker('scan-worker.js');
  } catch (e) {
    return;
  }
  const pipe = { worker: w, ready: false, busy: false, pending: null, inFlight: null, timer: 0 };
  scan.pipe = pipe;
  const off = () => {
    if (scan.pipe !== pipe) return;
    if (scan.pipeline) {
      workerFail('scan worker failed');
      return;
    }
    clearTimeout(pipe.timer);
    w.terminate();
    scan.pipe = null;
  };
  pipe.timer = setTimeout(() => { if (!pipe.ready) off(); }, WORKER_START_MS);
  w.onerror = off;
  w.onmessage = (e) => {
    if (scan.pipe !== pipe) return;
    const m = e.data || {};
    if (m.ready) {
      pipe.ready = true;
      clearTimeout(pipe.timer);
      pipeOn();
      return;
    }
    if (m.error) {
      off();
      return;
    }
    if (m.reply !== undefined) {
      reply(m);
      return;
    }
    pipe.busy = false;
    const done = pipe.inFlight;
    if (pipe.pending) {
      const next = pipe.pending;
      pipe.pending = null;
      pipePost(next);
    }
    for (const k of [...shown.frames.keys()]) if (k < done.seq) dropFrame(k);
    const seq = shown.frames.has(done.seq) ? done.seq : 0;
    if (scan.onFaces) scan.onFaces(done.faces, seq, shown.times.get(seq) || 0, m.scan || '');
  };
}

/** The scan worker takes the scan over: when both are ready and no command is waiting for its answer. */
function pipeOn() {
  const pipe = scan.pipe;
  if (scan.pipeline || !pipe || !pipe.ready || !scan.ready || scan.replies.size > 0) return;
  scan.pipeline = true;
  const r = scan.lastReset;
  pipe.worker.postMessage({ cmd: `adopt:${r.count}:${r.engine}`, id: 0 });
  scan.worker.postMessage({ cmd: 'role:find', id: 0 });
}

function pipePost(msg) {
  const pipe = scan.pipe;
  pipe.busy = true;
  pipe.inFlight = msg;
  pipe.worker.postMessage({ scanFaces: msg.faces, at: msg.at });
}

/** The finder's faces of picture [seq] on to the scan worker; a newer one waits in place of an older. */
function pipeQueue(msg) {
  const pipe = scan.pipe;
  if (!pipe.busy) {
    pipePost(msg);
    return;
  }
  if (pipe.pending && pipe.pending.seq) dropFrame(pipe.pending.seq);
  pipe.pending = msg;
}

function reply(m) {
  if (!m.reply) return;
  const done = scan.replies.get(m.reply);
  scan.replies.delete(m.reply);
  if (done) done(m.text || '');
}

/** Whether the scan runs in its own worker beside the finder (`scan-speed-up-4`). */
export function scanWorkerPipeline() {
  return scan.pipeline;
}

function workerPost(msg) {
  scan.busy = true;
  scan.inFlight = msg.seq || 0;
  const { seq, ...body } = msg;
  scan.worker.postMessage(body, msg.bitmap ? [msg.bitmap] : [msg.data]);
}

/**
 * Starts the worker; [onFaces] gets each picture's faces as numbers, its number (0: no read picture
 * kept), the ms its read picture's copy took and the worker's scan answer for it, [onFail] the reason
 * it cannot be used.
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
  pipeStart();
  scan.timer = setTimeout(() => { if (scan.worker === w && !scan.ready) workerFail('no answer'); }, WORKER_START_MS);
  w.onerror = (e) => { if (scan.worker === w) workerFail('error: ' + ((e && e.message) || 'load failed')); };
  w.onmessage = (e) => {
    if (scan.worker !== w) return;
    const m = e.data || {};
    if (m.ready) {
      scan.ready = true;
      clearTimeout(scan.timer);
      pipeOn();
      return;
    }
    if (m.error) {
      workerFail('error: ' + m.error);
      return;
    }
    if (m.reply !== undefined) {
      reply(m);
      pipeOn();
      return;
    }
    scan.busy = false;
    const answered = scan.inFlight;
    if (scan.pending) {
      const next = scan.pending;
      scan.pending = null;
      workerPost(next);
    }
    if (scan.pipeline) {
      pipeQueue({ faces: m.faces, at: m.at, seq: answered });
      return;
    }
    // Copies of earlier answers never shown go: the newest answer is the one to show.
    for (const k of [...shown.frames.keys()]) if (k < answered) dropFrame(k);
    const seq = shown.frames.has(answered) ? answered : 0;
    if (scan.onFaces) scan.onFaces(m.faces, seq, shown.times.get(seq) || 0, m.scan || '');
  };
}

/**
 * Sends the worker's scan a command (`reset:<engine>`, `outcome`; `scan-speed-up-2`); [onReply] gets its
 * answer, or '' when there is no worker.
 */
export function scanWorkerCommand(cmd, onReply) {
  if (!scanWorkerReady()) {
    onReply('');
    return;
  }
  const id = scan.nextReply++;
  // The scan worker gets the scan's commands once it runs the scan; a reset is kept for its taking over.
  const engine = cmd.startsWith('reset:') ? cmd.slice('reset:'.length) : null;
  scan.replies.set(id, (text) => {
    if (engine) scan.lastReset = { count: parseInt(text, 10) || 0, engine };
    onReply(text);
  });
  (scan.pipeline ? scan.pipe.worker : scan.worker).postMessage({ cmd, id });
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
    // The read picture's copy of the same video frame, drawn now (`scan-feedback`, `scan-read-picture-android`).
    const seq = ++shown.seq;
    const copied = copyFrame(v, x, y, w, h, seq);
    createImageBitmap(v, x, y, w, h, { resizeWidth: pw, resizeHeight: ph, resizeQuality: 'low' })
      .then((bitmap) => workerQueue({ bitmap, seq: copied ? seq : 0 }))
      .catch(() => { dropFrame(seq); sendCanvas(); });
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
  if (scan.pipe) {
    clearTimeout(scan.pipe.timer);
    scan.pipe.worker.terminate();
  }
  scan.pipe = null;
  scan.pipeline = false;
  for (const done of scan.replies.values()) done('');
  scan.replies.clear();
  scan.lastReset = { count: 0, engine: 'RULES' };
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

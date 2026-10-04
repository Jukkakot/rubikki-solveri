// Every browser call the app makes lives here (no inline js() in Kotlin: the minified build broke
// on it). Kotlin imports these through @JsModule("./platform.mjs"); only strings, numbers,
// booleans and JS objects cross the boundary.

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
 * sheet when the browser can share files; otherwise downloads the log as a text file.
 */
export function shareOrDownload(logName, logText, pictureNames, pictureData) {
  const files = [new File([logText], logName, { type: 'text/plain' })];
  const names = pictureNames ? pictureNames.split('\n') : [];
  const data = pictureData ? pictureData.split('\n') : [];
  names.forEach((name, i) => {
    const bytes = Uint8Array.from(atob(data[i]), (c) => c.charCodeAt(0));
    files.push(new File([bytes], name, { type: 'image/png' }));
  });
  if (navigator.canShare && navigator.share && navigator.canShare({ files })) {
    navigator.share({ files, title: logName }).catch(() => {});
    return;
  }
  const url = URL.createObjectURL(files[0]);
  const a = document.createElement('a');
  a.href = url;
  a.download = logName;
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

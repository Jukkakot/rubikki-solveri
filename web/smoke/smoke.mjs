// Smoke test of the production browser build:
//   node web/smoke/smoke.mjs web/build/dist/wasmJs/productionExecutable [outDir]
// Serves the build, opens it like a phone (412×915, Finnish), fails on any page error, waits for
// the app's "RUBIKKI ready" console line, saves home.png (must not be one flat colour), then opens
// ?selftest and checks the solver's speed. Screenshots go to outDir (default web/build/smoke).
// Uses Playwright's Chromium; PLAYWRIGHT_BROWSERS_PATH is respected.
import { chromium } from 'playwright';
import { mkdir } from 'node:fs/promises';
import { serve } from './serve.mjs';

const dist = process.argv[2] || 'web/build/dist/wasmJs/productionExecutable';
const out = process.argv[3] || 'web/build/smoke';
const LIMITS = { warmup: 4000, solve: 1500 };
const LESSONS_ROUTE = 'fi.jukkakot.rubikkisolveri.ui.nav.LessonsRoute';

function fail(message) {
  console.error('SMOKE FAIL ' + message);
  process.exitCode = 1;
}

await mkdir(out, { recursive: true });
const { server, url } = await serve(dist);
const browser = await chromium.launch({ executablePath: process.env.CHROMIUM_PATH || undefined });
try {
  const context = await browser.newContext({ viewport: { width: 412, height: 915 }, locale: 'fi-FI', deviceScaleFactor: 1 });
  const page = await context.newPage();
  const errors = [];
  page.on('pageerror', (e) => errors.push(e.message));
  const ready = page.waitForEvent('console', { predicate: (m) => m.text() === 'RUBIKKI ready', timeout: 60_000 });
  await page.goto(url);
  await ready;
  await page.waitForTimeout(1500);
  const shot = await page.screenshot({ path: `${out}/home.png` });
  const flat = await page.evaluate(async (b64) => {
    const img = new Image();
    img.src = 'data:image/png;base64,' + b64;
    await img.decode();
    const c = document.createElement('canvas');
    c.width = img.width; c.height = img.height;
    const g = c.getContext('2d');
    g.drawImage(img, 0, 0);
    const d = g.getImageData(0, 0, c.width, c.height).data;
    for (let i = 4; i < d.length; i += 4) {
      if (d[i] !== d[0] || d[i + 1] !== d[1] || d[i + 2] !== d[2]) return false;
    }
    return true;
  }, shot.toString('base64'));
  if (flat) fail('home.png is one flat colour');
  if (errors.length) fail('page errors: ' + errors.join(' | '));

  // Lost graphics (what a phone does to a background tab): the page reloads on the same screen,
  // without an error and without the "crashed last time" mark.
  const startLines = [];
  page.on('console', (m) => { if (m.text().includes('app.start')) startLines.push(m.text()); });
  const lessonsReady = page.waitForEvent('console', { predicate: (m) => m.text() === 'RUBIKKI ready', timeout: 60_000 });
  await page.goto(url + '#' + LESSONS_ROUTE);
  await page.reload();
  await lessonsReady;
  await page.waitForTimeout(500);
  const before = await page.evaluate(() => location.href);
  startLines.length = 0;
  const reloaded = page.waitForEvent('console', { predicate: (m) => m.text() === 'RUBIKKI ready', timeout: 60_000 });
  reloaded.catch(() => {});
  const lost = await page.evaluate(() => {
    // Compose puts its canvas in a shadow root.
    const canvases = [];
    const walk = (root) => root.querySelectorAll('*').forEach((el) => {
      if (el instanceof HTMLCanvasElement) canvases.push(el);
      if (el.shadowRoot) walk(el.shadowRoot);
    });
    walk(document);
    for (const c of canvases) {
      const gl = c.getContext('webgl2') || c.getContext('webgl');
      const ext = gl && gl.getExtension('WEBGL_lose_context');
      if (ext) { ext.loseContext(); return true; }
    }
    return false;
  });
  if (!lost) fail('no WebGL canvas to lose');
  else {
    await reloaded;
    await page.waitForTimeout(1000);
    const after = await page.evaluate(() => location.href);
    if (after !== before) fail(`after lost graphics on ${after}, expected ${before}`);
    if (startLines.length !== 1) fail(`expected one reload after lost graphics, saw ${startLines.length}`);
    else if (startLines[0].includes('crashedLastTime=true')) fail('lost graphics counted as a crash');
    if (errors.length) fail('page errors after lost graphics: ' + errors.join(' | '));
  }

  const self = await context.newPage();
  self.on('pageerror', (e) => fail('selftest page error: ' + e.message));
  const line = self.waitForEvent('console', { predicate: (m) => m.text().startsWith('SELFTEST'), timeout: 120_000 });
  await self.goto(url + '?selftest');
  const text = (await line).text();
  console.log(text);
  const m = text.match(/^SELFTEST ok warmup=(\d+) solve=(\d+) beginner=(\d+) rotation=(\d+)/);
  if (!m) fail('self-test: ' + text);
  else {
    if (Number(m[1]) > LIMITS.warmup) fail(`warm-up ${m[1]} ms > ${LIMITS.warmup}`);
    if (Number(m[2]) > LIMITS.solve) fail(`solve ${m[2]} ms > ${LIMITS.solve}`);
  }
  if (!process.exitCode) console.log('SMOKE OK');
} catch (e) {
  fail(e.stack || String(e));
} finally {
  await browser.close();
  server.close();
}

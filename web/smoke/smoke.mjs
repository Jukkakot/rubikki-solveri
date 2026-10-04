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

function fail(message) {
  console.error('SMOKE FAIL ' + message);
  process.exitCode = 1;
}

await mkdir(out, { recursive: true });
const { server, url } = await serve(dist);
const browser = await chromium.launch();
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

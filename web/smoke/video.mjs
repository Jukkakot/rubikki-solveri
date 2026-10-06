// The video scan in the browser build with a fake camera (Chromium's own test pattern, or a .y4m
// video of a cube):
//   node web/smoke/video.mjs web/build/dist/wasmJs/productionExecutable [video.y4m] [--no-worker]
// Opens the video scan like a phone, runs it for a while and fails on any page error or when the
// log has no `scan.camera` line or no snapshot. Prints the scan's log lines. With --no-worker the
// worker's script is blocked, so the scan must fall back to the page's own thread.
import { chromium } from 'playwright';
import { serve } from './serve.mjs';

const args = process.argv.slice(2);
const noWorker = args.includes('--no-worker');
const [dist = 'web/build/dist/wasmJs/productionExecutable', video] = args.filter((a) => !a.startsWith('--'));
const VIDEO_ROUTE = 'fi.jukkakot.rubikkisolveri.ui.nav.VideoScanRoute';
const RUN_MILLIS = 9_000;

function fail(message) {
  console.error('VIDEO FAIL ' + message);
  process.exitCode = 1;
}

const { server, url } = await serve(dist);
const flags = ['--use-fake-device-for-media-stream', '--use-fake-ui-for-media-stream'];
if (video) flags.push(`--use-file-for-fake-video-capture=${video}`);
// CHROMIUM_PATH: a Chromium other than Playwright's own download.
const browser = await chromium.launch({ args: flags, executablePath: process.env.CHROMIUM_PATH || undefined });
try {
  const context = await browser.newContext({ viewport: { width: 412, height: 915 }, locale: 'fi-FI', deviceScaleFactor: 1 });
  await context.grantPermissions(['camera']);
  if (noWorker) await context.route('**/scan-worker*', (route) => route.abort());
  const page = await context.newPage();
  const errors = [];
  const lines = [];
  page.on('pageerror', (e) => errors.push(e.message));
  page.on('console', (m) => { if (m.text().includes('scan.')) lines.push(m.text()); });
  const ready = page.waitForEvent('console', { predicate: (m) => m.text() === 'RUBIKKI ready', timeout: 60_000 });
  await page.goto(url + '#' + VIDEO_ROUTE);
  await ready;
  await page.waitForTimeout(RUN_MILLIS);
  // Leaving the screen writes the last snapshot.
  await page.goto(url + '#fi.jukkakot.rubikkisolveri.ui.nav.HomeRoute');
  await page.waitForTimeout(500);
  lines.forEach((l) => console.log(l));
  if (errors.length) fail('page errors: ' + errors.join(' | '));
  if (!lines.some((l) => l.includes('scan.camera'))) fail('no scan.camera line');
  const snapshots = lines.filter((l) => l.includes('kind=snapshot'));
  if (snapshots.length === 0) fail('no snapshot');
  const worker = lines.find((l) => l.includes('scan.worker'));
  if (worker) {
    const expect = noWorker ? 'worker=false' : 'worker=true';
    if (!snapshots.every((l) => l.includes(expect))) fail(`snapshots without ${expect}`);
  }
  if (!process.exitCode) console.log('VIDEO OK');
} catch (e) {
  fail(e.stack || String(e));
} finally {
  await browser.close();
  server.close();
}

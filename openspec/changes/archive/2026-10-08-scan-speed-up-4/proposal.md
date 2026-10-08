# Proposal: scan-speed-up-4

## Why

Browser phone test 2026-10-08 19:09 (version 339): both video scans finished right in 11 and 16 s, and the
scan logic no longer grows over a scan (`scan-speed-up-3`). In the busiest phase, though (two faces in
view, just before the finish), the scan logic takes 10–20 ms a picture. In the browser it runs in the same
worker after the face finder (13–19 ms), so the two add up to 25–35 ms and the pictures a second drop from
21–26 to 16–18. On the JVM the busy windows of the long recordings cost 3–5 ms a picture (up to 17 ms).
A profile of `web_121505` puts 30 % in the best cube's per-place margins (40 searches a cube: two for each
of the 20 corner and edge places) and 18 % in the cube costs of the turn trials.

## What Changes

- **Margins only as far as the scan looks.** Every decision the scan makes from a margin compares it with
  one threshold (2.0: a sticker known, the cube clear). The searches currently work margins out to 15.
  The scan's best cube works them out only up to the threshold. The decisions stay exactly the same, and
  the log's `margin` then shows at most the threshold. Phone and browser both gain.
- **The browser's two jobs side by side.** Finding faces and the scan logic run in two workers, one after
  the other like a pipeline: while the scan works on one picture, the finder reads the next. The pictures
  a second are then set by the slower of the two, not by their sum. The phone app already works this way
  (its scan has its own thread).
- If the first step leaves the margins as the largest cost, one shared search gives all places'
  margins instead of 40 separate ones (measured first; same results required).

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `video-scan`: "Reading stays quick in a long scan" also covers the busy moments (two faces in view),
  and in the browser finding and scanning run side by side.

## Impact

- `cube`: `scan/BestCube.kt` (margin limit as a parameter; maybe the shared search), `scan/VideoScan.kt`
  (passes its threshold), tests in `BestCubeTest`, `RulesScanTest`.
- `webworker` and `web`: a second worker role (scan only), `platform.mjs` passes the faces from the finder
  worker to the scan worker and the answers to the page; the read-picture canvas pool grows by one or two
  pictures in flight. Fallbacks stay: no worker → the page reads as before.
- `app` (Android): no code change; it gains from the first step.

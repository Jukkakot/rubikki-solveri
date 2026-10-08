## 1. Margins only as far as the scan looks (cube)

- [x] 1.1 Baseline: scratch replay of all fixtures and the bench windows of `web_121505` and `20261007_202403`, profile shares; into `design.md` Findings
- [x] 1.2 `BestCube.solve` takes a margin cap (default `MARGIN_CAP`); both scanners in `VideoScan` pass `CLEAR_MARGIN + 0.5`; `VideoScanLog` notes the capped margin
- [x] 1.3 Replay matches line for line (clearness compared as `min(cl, 2.5)`); `BestCubeTest`, `RulesScanTest` and the budget test pass; a `BestCubeTest` case that the capped margins decide like the full ones on a few evidences; bench and profile again into Findings
- [x] 1.4 (Not needed: about 14 % after the cap; Findings) Only if `alternative()` is still above about 15 % of the busy windows: the shared margin search (design 2), same replay, numbers into Findings; dropped otherwise

## 2. Browser pipeline (webworker, web, shared)

- [x] 2.1 `ScanWorker.kt`: a role from the first message (`find`: faces only; `scan`: faces in, scan state out, with its commands); both roles in one `scan-worker.js`
- [x] 2.2 `platform.mjs`: start the scan worker beside the finder; faces forwarded with the picture's number and time; newest-only waiting at each stage; canvas pool of 5; a picture shown on its scan answer; fallback to the single worker when the scan worker fails (`scan.worker` line with `pipeline=`)
- [x] 2.3 Kotlin side (`Camera.wasmJs.kt`, `VideoScanScreen`): the answer path unchanged for the screen; `fps` counts scan answers
- [x] 2.4 `web/smoke/video.mjs` in Chromium: the video scan finishes with the pipeline, and with `--no-worker`; `./gradlew` web build and the checks green

## 3. Docs and roadmap

- [x] 3.1 `docs/architecture.md` (worker paragraph: two roles, the pipeline; the margin cap); roadmap item 70 `scan-speed-up-4` done, with what the user checks on the phone (fps with two faces in view)

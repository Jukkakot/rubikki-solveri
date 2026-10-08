# Design: scan-speed-up-4

## Context

See `proposal.md` (Why). Measured 2026-10-08 (JVM bench of `web_121505`, 100-picture windows): pictures
with faces 2–5 ms on average, single pictures up to 17 ms. The profile: `BestCube.solve` 30 %, nearly all
of it `alternative()` (per corner and edge place, two Murty walks: another piece there, the same piece
turned otherwise). The turn trials' `BestCube.cost` take 18 %, `assignOpen` 17 % and `recheck` 13 %.
Every use of a margin in the scan compares it with `VideoScan.CLEAR_MARGIN` (2.0): `supportedMargin >=`
for a known sticker, `clearness >=` for the finish, `margin(i) <` for a shown reading. The value itself goes
only to the log (`margin=`) and to the replay's `cl=`. `MARGIN_CAP` is 15.

In the browser (`scan-speed-up-2`) one worker (`scan-worker.js`, `ScanWorker.kt`) finds the faces and then
runs `VideoScan.onFrame` on them. It answers both together, and the page sends the next picture only then
(`platform.mjs`: `busy`/`pending`, only the newest picture waits). Three box-sized canvases hold the read
pictures until their answer comes (`shown.frames`).

## Goals / Non-Goals

**Goals:**
- The scan decides exactly as today: the replay of all fixtures (`Replay.java` in the scratchpad, `cl=`
  left out or capped alike) matches line for line after the margin step.
- JVM: the busy windows of `web_121505` (pictures 100–299) at least a third cheaper. The budget test
  (5 ms a window) keeps passing.
- Browser: with two faces in view, the pictures a second stay close to the finder's own rate (about 1000 /
  `finderMs`), not 1000 / (`finderMs` + `scanMs`). The user checks it on the phone (log snapshots).

**Non-Goals:**
- No change to what the scan decides, its thresholds or the finder.
- No new Android threading (its scan already has its own thread).

## Decisions

1. **Margin limit as a parameter.** `BestCube.solve(evidence, scheme, cap = MARGIN_CAP)`: the searches stop
   at `cost + cap`, and a larger margin reads as `cap`. Both scanners in `VideoScan` pass
   `CLEAR_MARGIN + 0.5`. The half point keeps a margin of exactly 2.0 clear of rounding at the limit
   (`limit - cost` can come back as 1.9999999). Every comparison with 2.0 then has the same answer. The
   Murty walks visit the same permutations in the same order up to the lower limit, so each value under
   the cap is the same number too. `BestCubeTest` keeps the default cap (its margins of 3 and more). The log's
   `margin=` shows at most 2.5. A note in `VideoScanLog` says so.
   Alternative: compute margins lazily only for stickers asked about. Rejected: the finish asks for all.

2. **Shared margin search, only if needed.** After decision 1, profile again. If `alternative()` is still
   above about 15 % of the busy windows, all places' margins come from one walk: the permutations cheapest
   first up to the limit, and for each one the per-place best with another piece and with another turn (the
   turn DP with one place's turn banned), until every place has its answer or the next bound passes it. It
   must give the same numbers on the replay; otherwise it is dropped. The findings say which way it went.

3. **Browser: two workers, a pipeline.** The same `scan-worker.js` is started twice and told its role by its
   first message: `find` (faces only) or `scan` (the `VideoScan`, its commands `reset:`/`outcome`).
   - The page sends a picture to the finder whenever the finder is free (newest waits, as today), then
     forwards the faces it gets back (`FaceCodec` text, small) to the scan worker with the picture's number
     and the time it was found. When the scan worker is busy only the newest faces wait. The faces of a
     dropped picture are never scanned, and its read canvas is freed.
   - A picture is shown when its scan answer comes back: the marks still lie on the picture they were read
     from. The read-picture pool grows from 3 to 5 canvases: one picture in the finder and one waiting, one
     in the scan and one waiting, and the one on show.
   - `fps` in the snapshot counts the scan's answers (the pictures shown and scanned). `finderMs` and
     `scanMs` stay as they are.
   - If the scan worker cannot start or throws, the finder worker runs the scan as today: one worker,
     answers together. The `scan.worker` line says `pipeline=true/false`.
   Alternative: the finder worker posts faces straight to the scan worker over a `MessageChannel`. It saves
   one hop through the page, but the page must still match answers to its canvases. Rejected: more moving
   parts for well under a millisecond.
   Alternative: a time-based rate limit on the best cube. Rejected: it changes when the scan finishes.

## Risks / Trade-offs

- [A scan that falls behind drops pictures (only the newest waits): tracks see fewer pictures] → a face
  track bridges gaps up to `Tracker.GAP_MILLIS`, and the scan already lost the same pictures when the
  single worker was busy. The phone test shows whether finishing gets slower.
- [Two Wasm instances load twice: start time and memory] → the worker's `startMs` is in the log (about
  400 ms today). Both start in parallel. Memory of a second module instance is a few MB.
- [Margins capped in the log make the snapshots' `margin=` less informative] → it was read only as "clear or
  not" when tuning.

## Findings

(filled in during apply)

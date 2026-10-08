# Proposal

## Why

Third phone test (web, 1.0.301, 2026-10-08 09:10–09:15 UTC, the striped cube; log and screen
recording in `testdata/video/2026-10-08c/`): four rules scans and none finished. The first knew the
true cube in 20 s (09:11:17, margin 3.6) and then waited 30 s until "stuck". The look scanner finished
one of three.

- **Never finished with the cube known:** `complete` must hold for half a second (`FINISH_MILLIS`),
  and every new face track that is not settled yet (`unsureFaces`, `settledFor`) revokes it. A cube
  turned in the hand gives a new track every few frames, so the half second never comes; the log
  shows "every side done" three times in four seconds. The spec already says the scan shall never
  stay with everything read and nothing happening.
- **Slower and slower:** the rules scanner went from 14 to 5 pictures a second while about 30 face
  tracks piled up; tracks are never dropped and the per-frame work goes over all of them (pair
  costs, tables, rechecks). Slow reading makes the scan feel sticky and makes the first problem
  worse.

The user wants the hardest cube (striped) to scan well, and after this fix ideas for making the
algorithm faster.

## What Changes

- Once the cube is clear, a face newly in view does not hold the finish back unless it reads
  against the cube (more than one sticker otherwise in its best way); the half second keeps
  counting. A face that does read against it, or a drop in clearness, still revokes it.
- Face tracks that ended do not cost per-frame work forever: an ended short track that never
  settled is dropped, and the per-frame work runs over a bounded number of tracks per face (the
  rest kept only as the votes they already gave). The harness results must not get worse.
- The camera part of today's screen recording becomes a fixture (never wrong), plus a synthetic test
  for the finish: a clear cube followed by a new face track finishes.
- After the fix: measure where a frame's time goes and bring the user a short list of speed-up
  ideas (no code for them in this change).

## Capabilities

### Modified Capabilities
- `video-scan`: the finish is not held back by a new face that fits the clear cube; reading stays as
  quick late in a long scan as at its start.

## Decisions

- **What revoked the finish:** replaying the phone fixture past clear with the faces moved (new tracks), a
  new track in its first counted frame has its face settled but its turn open and reads against the cube
  in the guessed turn (here a lattice shifted by a row); next frame it settles. Each such frame reset the
  half second. (The log's `?` centres were mostly tracks taken for no face, not open ones.)
- **Fix (`FaceTracks.quietFor`, used once `complete` held):** an unsettled track holds the finish back only
  when it has at least `KEEP_READINGS` readings and reads against the cube (more than one sticker
  otherwise) in every turn of every face it could be. Broader than task 1.2's wording (any turn, and short
  tracks never hold): a stray lattice for a moment is not "a face that reads against it"; whether the cube
  stays right is left to the clearness and `turnsClear`, re-checked every frame. Acceptance: never wrong.
- **Measured (2.1, fixture ×3, JVM):** `web_084657` 2nd scan 9.9 → 18.1 ms/frame (66 → 198 tracks);
  `132721` 2.4 → 5.2 ms (49 → 145); `202058` 7.8 → 27.2 ms (36 → 108). Profiling showed two causes: tracks
  piling up (recheck, voting, evidence go over all), and `turnsClear` (16 ms) once the cube was clear but a
  pair of turns was not, trying dozens of cube costs every frame before the failing one.
- **Speed (2.2):** ended tracks retire (`FaceTracks.retire`): never counted; taken for no face 2 s ago;
  settled and older than the newest `MAX_FACE_READINGS` of their face's evidence (they gave no evidence any
  more). Unsettled ones stay (task 2.2 said to drop the short ones): dropping them lost `202403` (striped U2,
  dim; two cubes equally good after), and the tracks stay bounded without it. A retired voting track keeps its votes in the tables (`archived`) and
  its centre in the references. `turnsClear` tries last frame's failing turns first (same answer). After:
  `web_084657` 6.7 → 8.7 ms (29 → 34 tracks), `132721` 2.1 → 2.9 (27 → 26), `202058` 7.7 → 9.1 (19 → 26).
  The test compares the second and third time through, not the first: the first holds the cheap start.

## Speed-up ideas (4.2, for the user to choose; no code here)

Profile after the fix (JVM, late in `202058`, ~9 ms a frame): best-cube turns of faces seen alone
(`settleTurns`) 5 ms, `BestCube.solve` 1.8, recheck 1.0, `turnsClear` 0.7, the rest under 0.1 each. On the
phone (log): the finder 46 ms a frame on average, at about 11 frames a second in all.

1. Finder (about half the phone's frame): search near last frame's faces first, or a smaller picture
   while faces are followed. Biggest gain, maybe 1.5–2× the frame rate; needs phone checks.
2. `settleTurns`: sum each face's votes once per frame and turn the sums, not rebuild the evidence from
   every reading for each of its ~30 trials; skip it when those faces got no new reading. 5 → ~1 ms.
3. `BestCube.solve` / `turnsClear`: reuse the last result when the evidence moved little. ~2 ms.
4. Paint: not measured; add its time to the log's snapshots first.

## Impact

`cube/scan/FaceTracks.kt`, `VideoScan.rulesFrame`, possibly `Tracks.kt`; tests and fixtures in
`cube/src/jvmTest`.

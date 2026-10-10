# Proposal

## Why

Browser scan 2026-10-10 10:25 (recording `web_20261010_102548`, committed) never finished. The best
cube was complete (54 known, valid, margin 2.5, the maximum) for 33 s, the side row showed 5/6, and
the user gave up. The replay reproduces it: `complete` stays false because of followed faces
(tracks) that are not settled.

- `#0`: white, its turn never told. It reads exactly like the white side of the clear cube in one
  turn (8/8).
- `#3`: white, 9 readings, two stickers against the cube (6/8), while two other white tracks
  (32 and 40 readings) fit it 8/8.

The first completion uses `settledFor` (every holding track settled, or short), so an old track with
an open turn blocks forever. Only after a completion does the looser `quietFor` apply. This breaks
"the scan SHALL never stay with everything read and nothing happening" and the user's principle
that a wrong observation must not hold the rest.

## What Changes

- While the best cube is clear, an unsettled track that reads like the cube in some turn of a face
  it could be no longer holds the finish. The `quietFor` rule also applies to the first completion.
- A track that reads against the clear cube (two or more stickers in every turn) counts as an
  outvoted misread when the same side's tracks that fit the cube have at least 3× its readings. It
  no longer holds the finish (and no longer counts as evidence).
- Never finish with a cube that breaks a rule. The finish still needs the cube clear for half a
  second, so a single misread cannot finish a wrong cube.

## Capabilities

### Modified Capabilities

- `video-scan`: "Finish the video scan" (open or outvoted tracks do not hold a clear cube; the
  look-alike scenario names a side instead of "turn the cube").

## Impact

- `cube`: `FaceTracks` (`settledFor`/`quietFor`, an outvoted-track check) and `VideoScan.onFrame`
  (the `quiet` condition).
- Tests: the new recording finishes with `OYGYWWRORWRWGRBRYYGGBOGROGGYWYRYGBOGBRWOOBRBBOBWWBWOYY`.
  The earlier recordings and every fixture keep their finishes and cubes, with no wrong finish,
  normal and robustness variants alike.

## Implementation notes

- `VideoScan.onFrame`: `quiet = quietFor(best.cube)` before and after the first completion. The
  first completion no longer also needs every face told (`unsureFaces` empty): an open-face track
  that fits the clear cube on a face it could be does not hold it either, as the spec says. Tried
  both ways; neither gave a wrong finish, the looser one finishes one robustness fixture earlier.
- `settledFor` removed (no longer used). `FaceTracks.outvoted`: the track reads against the cube in
  every turn of every face it could be, and on each of those faces the other tracks *settled* to that
  face that fit the cube have `OUTVOTE` (3) times its readings. Red read for orange counts as a
  mismatch here (strict, as `fits`).
- Evidence: an outvoted track leaves `voting` only against the last best cube that was clear before
  the picture's work, so a misread is dropped only once the cube is already clear without it.
- Recording `web_20261010_102548`: never finished → finishes at picture 457 (about 15.6 s after the
  reset; clear from about 5.5 s) with the true cube. It waits until the old track `#0` reads like
  the cube again (its warm stickers renamed once both warm centres are known): before that `#0` and
  `#3` together weigh 23 readings against the fitting track's 40, not outvoted.
- Earlier recordings: `web_20261009_100814` 151 → 151, `web_20261009_100824` 202 → 202, both right.
- Acceptance harness (finish picture; all finishes right before and after, no wrong finish):
  as recorded unchanged on all 18 fixtures; robustness unchanged except `20261007_132721`
  275 → 266 and `web_181940` 381 → 380.

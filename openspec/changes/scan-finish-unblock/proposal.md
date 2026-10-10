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

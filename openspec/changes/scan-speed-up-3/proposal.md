# Proposal: scan-speed-up-3

## Why

The video scan's logic gets slower as the scan goes on, against the `video-scan` requirement
"Reading stays quick in a long scan". Browser 1.0.333: `scanMs` 9 → 37 ms, pictures a second 24 → 14.
Replaying the long phone fixtures on the JVM (explore, 2026-10-08) shows the same:

| fixture | first 100 pictures | from picture 200 on | tracks worked through |
|---|---|---|---|
| `web_121505` (never finishes, open tracks) | 2.2 ms | 8–11 ms | 7 → 30 |
| `PHONE_SCAN_2` (`web_084657:499-998`) | 1.5 ms | 7 ms | 7 → 30 |

A picture with no face in it costs as much as one with faces: every picture works the whole state out
again (`recheck`, `assignOpen`, `settleTurns`, `BestCube.solve`), although only the track in view (one
or two) got a new reading. Late in a scan the profile is `assignOpen`/`pairCost` ~54 % (`web_121505`,
up to 8 open tracks × 28 pairs × 25 × 25 options), `settleTurns` ~42 % and `BestCube.solve` ~34 %
(`PHONE_SCAN_2`; `settleTurns` runs up to ~36 best-cube searches a picture). The test meant to catch
this (`aLongScanReadsAsQuicklyLateAsEarly`) compares the third time through a recording with the
second, when the cost has already levelled off, so it passed.

## What Changes

All three, measured with the replay after each (user, 2026-10-08):

- **A. Nothing changed, nothing redone:** a picture that gives no track a reading and moves no track
  across a time limit (`Tracker.GAP_MILLIS`, `RETIRE_MILLIS`) keeps the last frame's assignments,
  evidence and best cube.
- **B. Work kept between pictures:** what depends on one track or one pair of tracks (a track's
  votes, a pair's cost table, the pair rules' bans) is kept and worked out again only for tracks that
  got readings; everything is worked out again when the colour references change (every reading is
  named again then). `recheck`'s per-track tables come from the full table minus the track, not from
  summing all tracks again.
- **C. Fewer best-cube searches:** `settleTurns` remembers the cost of each turn combination it tried
  in a picture and starts from the last picture's turns; the best cube and the turns are searched
  again only when the evidence changed (details and any rate limit in `design.md`).
- **Test:** the long-scan test gets an absolute budget instead of comparing late with later: on the
  long phone fixtures no 100-picture window averages over 5 ms a picture on the JVM (about 15 ms in the
  browser), and a picture without faces late in the scan costs a small fraction of that.

Behaviour stays as specced: same faces, same cube, same finish timing on the fixtures (the existing
scan tests guard it).

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- none (the `video-scan` requirement "Reading stays quick in a long scan" already says this; the
  change makes the code meet it; `skip_specs: true`)

## Impact

- `cube`: `scan/FaceTracks.kt` (caches, skip, `settleTurns`), possibly `scan/BestCube.kt` and
  `scan/PairRules.kt`; `jvmTest` `RulesScanTest.kt` (the long-scan test).
- `app`, `shared`, `web`: none (they call `VideoScan.onFrame` as before; `scanMs` in the log shows the
  result).

# Proposal

## Why

Browser phone test 2026-10-09 (10:30–10:33, version 348): four scans of the same cube all finished
right. The rules scanner finished in about 4 and 7 s with `scanMs` flat at 6–9 ms. The earlier
("look") scanner failed its first try in 20 s, with progress going back (U 9 → 5), and its
`scanMs` grew from 8 to 23 ms. The user decided to drop the earlier scanner.

The same recording shows grey veils floating beside and above the cube while it is turned
quickly. A lattice found in a single blurred picture, which no earlier picture followed, gets the
full set of nine veils for that picture. A few of these in a row look like grey ghost tiles in the
air. The spec already says such a lattice gets no outline; its veils still show.

## What Changes

- The earlier video scanner and its Settings choice are removed. The rules scanner is the only
  video scanner. A stored choice of the earlier one is ignored. Log lines no longer carry the scanner name.
- A face found in the picture gets marks (veils, rings, dots) only once it has been followed from
  an earlier picture. A lattice found in one picture alone shows nothing. Outlines and ticks stay
  as they are.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `video-scan`: "Two scanners to compare" removed; "Progress on the real cube" says marks only on faces followed from an earlier picture.

## Impact

- `cube`: `VideoScan` loses the earlier scanner's path and the engine choice. `FoundFace` tells
  whether its face is followed. Tests and harnesses for the earlier scanner are removed or moved
  to the rules scanner.
- `shared`/`app`/`web`/`webworker`: the Settings row, the stored choice, the engine passed to the
  scan, and the worker's engine message all go. The paint skips faces that are not followed.
- `docs/architecture.md`: the two-scanner section is cut to the one scanner.

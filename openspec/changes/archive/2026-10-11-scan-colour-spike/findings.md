# Findings (2026-10-11)

`ColourSpikeHarness` (`COLOUR_SPIKE=1`). Stills: the finder at 240 px as in the app, 10 pictures a
second. Recordings: the faces as recorded, at their own times. "Stickers right" and "red↔orange
wrong" name each reading by the source's own true centres (best-case references, as
`ReadingMeasure`). The striped-cube sources score low on them because their faces match the truth
in several turns, so for those only the scan columns count. B2 is B pulling only readings at
least a quarter of the two groups' distance away from the split.

## Scan outcome (time to finish; W = finished with a wrong cube)

| Source | today | A | B | A+B | B2 |
|---|---|---|---|---|---|
| 20261005_151828 | 22.6 | 22.6 | 22.6 | 22.6 | 22.6 |
| 20261005_151903 | 11.7 | 11.7 | 11.7 | 11.7 | 11.7 |
| 20261005_213729 (evening) | 12.2 | never | 12.2 | never | 12.2 |
| 20261005_213817 (evening) | 12.3 | 12.3 | 12.3 | 12.3 | 12.3 |
| 20261005_213850 (evening) | 16.5 | 16.7 | 16.3 | 16.3 | 16.3 |
| 20261005_213929 (evening) | 19.0 | 19.0 | 19.0 | 19.0 | 19.0 |
| 20261007_152753 | 7.2 | 7.1 | 6.6 | 6.6 | 6.6 |
| 20261007_202058 (striped, dim) | never | never | never | never | never |
| 20261007_202156 (striped, table) | 18.0 W | 20.5 | 20.4 W | 33.3 | 20.4 W |
| 20261007_202318 (striped U2) | never | never | 14.6 | 20.5 | 20.5 |
| 20261007_202403 (striped U2, dim) | never | 12.3 | 13.9 | 13.9 | 13.9 |
| web_181940 | 11.1 | 11.4 | 11.0 | 11.0 | 11.0 |
| web_084657:66 | 10.3 | 10.3 | 10.3 | 10.3 | 10.3 |
| web_121505:66 | never | never | never | never | never |
| web_121505:448 | never | never | 13.7 W | never | never |
| rec web_20261009_100814 | 6.2 | – | 5.2 | – | 5.2 |
| rec web_20261009_100824 | 9.2 | – | 11.1 | – | 11.1 |
| rec web_20261010_102548 | 7.8 | – | 9.8 | – | 10.9 |
| rec web_20261011_100403 (stuck) | never | – | never | – | never |
| rec web_20261011_101618 (stuck) | never | – | 12.5 | – | 16.3 |

## Readings (camera sources and recordings)

| Source | right today / A / B | red↔orange wrong today / A / B |
|---|---|---|
| 20261005_151903 | 100.0 / 98.0 / 100.0 % | 0.0 / 3.4 / 0.0 % |
| 20261005_213817 | 98.7 / 97.4 / 99.2 % | 0.7 / 2.0 / 0.0 % |
| 20261005_213850 | 89.2 / 88.3 / 89.0 % | 31.1 / 31.1 / 31.8 % |
| 20261005_213929 | 94.2 / 91.9 / 96.4 % | 17.5 / 20.6 / 10.0 % |
| rec 20261009_100814 | 96.8 / – / 98.1 % | 8.0 / – / 4.3 % |
| rec 20261010_102548 | 98.1 / – / 93.6 % | 4.2 / – / 9.5 % |
| rec 20261011_100403 | 98.9 / – / 99.5 % | 2.2 / – / 0.0 % |
| rec 20261011_101618 | 94.4 / – / 94.6 % | 15.3 / – / 14.7 % |

A's time per picture was not kept from the run; with A a no-go it is not needed.

## What it shows

- **A (colours from the full still): no-go.** On these stills (640 px long side) it reads no better,
  slightly worse on most camera sources. One evening video stops finishing. Two striped sources do
  better (one never finished before, one finished wrong), so the 240 px blobs are not the main
  error. The 1280 px live camera could still differ, but nothing here argues for building it.
- **B (global red/orange split): no-go as built.** It finishes the stuck 10:16 scan (12.5 s, the
  right cube) and two striped videos that never finished, but makes one source finish wrong
  (`web_121505:448`). It also makes two good recordings 20–40 % slower (`100824` 9.2 → 11.1 s,
  `102548` 7.8 → 9.8 s). There it reads worse: 98.1 → 93.6 % right. One hue cut for the whole
  cube fails when the light differs between faces: a shaded orange face sits below the cut while a
  lit red face sits above it. B2 avoids the wrong cube, but is slower still.
- **The stuck scans are a references problem, not a readings problem.** With the true centres as
  references, the 10:04 recording's warm stickers are only 2.2 % wrong, but the scan named B's and
  F's oranges red throughout (`RecordingReplay`: B `YRRYBRBBW`, F `BRGRGRROY`). The readings separate. The
  scan's red and orange references, taken from the two centres under each centre's own light, put
  the split in the wrong place for the other faces. B fixed the references only by moving every
  reading, which also hurt the good scans.

## Follow-up

`scan-warm-references`: build the fix inside the scan's references, not as a recolouring of the
readings. Set red and orange from the scan's settled faces, using each face's own warm stickers
next to its centre, so a face in shade is judged against that shade. Check it with the same
harness and go rules (stuck recordings finish, no wrong cube, no source more than 10 % slower).
Idea A is dropped unless a 1280 px recording shows blob colour errors.

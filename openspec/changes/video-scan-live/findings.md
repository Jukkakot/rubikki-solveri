# Findings: smaller pieces (spike 2.1)

Measured 2026-10-05 with `VideoScanHarness.spikePieces` (since removed, see below) on the frames of
both test videos: `FaceFinder` with its hit floor lowered, every face and piece of at least the
floor's stickers (the centre among them) fed to `VideoScan` as partial faces, 10 fps.

| video | floor | pieces fed | final cube | most wrong while complete | frames to complete | recognised at halfway |
|---|---|---|---|---|---|---|
| angled (151828) | 9 (full only) | 0 | true | 0 | 226 | 45 |
| angled | 7 (this change) | 223 | true | 0 | 226 | 45 |
| angled | 6 | 272 | true | 0 | 226 | 45 |
| angled | 5 | 341 | true | 0 | 226 | 45 |
| angled | 4 | 468 | true | 0 | 226 | 45 |
| straight (151903) | 9 (full only) | 0 | true | 0 | 143 | 36 |
| straight | 7 (this change) | 54 | true | 0 | 143 | 36 |
| straight | 6 | 62 | true | 0 | 143 | 36 |
| straight | 5 | 165 | true | 0 | 143 | 36 |
| straight | 4 | 266 | true | 0 | 143 | 36 |

## Decision: no-go

Pieces add no wrong stickers, but they do not speed up the scan on either video, so by the
criterion in `design.md` the floor stays at 7 and the setting is removed.

Why: a face's group starts only from a full face, and on both videos every face is seen fully
within a few frames of first coming into view; recognition then advances a whole face at a time
(the replay's count goes 9, 18, 27, …). Pieces only add votes where full faces already vote. Even
the 7–8 sticker faces of task 1 change only a few single frames (a face partly recognised one or
two frames earlier), not the frames to complete.

Not measured: rows without the centre (a row next to a face seen in the same frame); they cannot be
told apart by centre colour and would need their own grouping, for the same expected (non-)gain.

Worth revisiting only if a later video shows a face that is never seen fully (always half hidden),
or if groups may start from a partial face.

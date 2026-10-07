# Design

## Context

See proposal.md (Why). Evidence is the log `testdata/video/2026-10-07/web-181940/log.txt`
(snapshots every 2 s between 15:18:06 and 15:19:20 UTC; video second = UTC − 15:18:19). From 15:18:36
the net was right except the R face: `B?G???B?G`, centre unknown, margin about 1.2–1.5, so
`complete` (no doubtful pile with votes, clearness ≥ `CLEAR_MARGIN`) never held. At 15:18:44 and
15:19:00 the side faces fell to their middle columns only (`F3 B3`, `R0 L0`): with the red pile out
of `groups`, the neighbour views that fixed the side faces' rotations stopped counting, the
rotations became unsettled, and only rotation-invariant stickers stayed known.

The log has no per-frame readings, so which branch of `VideoScan.nameJointly` kept the red pile
doubtful is not certain: either the joint naming (red↔orange swap within `DOUBT_MARGIN`) or the
"stray" rule for a pile with fewer than `MIN_VOTES` inliers whose own nearest colour (orange) is
taken (`left[0].key != own`). The first task reproduces it with synthetic readings before fixing.

## Goals / Non-Goals

**Goals:** the red face's case finishes; known stickers do not vanish on a rename; the user sees the
read colours again; marks stay on a hand-held cube.

**Non-Goals:** orange read as red on single stickers (the scan already takes the piece that fits
the rest); a mirrored lattice on lone faces and the lattice shifted by a row (the white stickers on
the orange face at 0 s): left for a later change if they show up again once the red case is fixed.

## Decisions

1. **Sixth colour by elimination.** When five piles are named and not doubtful, a sixth pile with
   readings (any number of inliers) is named with the one colour left and is not doubtful, whatever
   its centre's nearest colour. Its centre still has to differ clearly from the five (it is a pile
   of its own, as now), so a stray lattice across an edge, which piles with an existing face, is not
   affected. Alternative: widen the red/orange doubt margin; rejected, it would not help other pairs
   (white/blue, yellow/white) and still leaves a sixth face waiting.
2. **Renames keep `sticky`.** The `sticky.fill(null)` on rename goes. Keeping it is safe: a sticky
   colour holds only while its votes still lead (`v[sticky] >= v[lead]`), and the votes are worked
   out again from the readings renamed with the new centre references. Alternative: keep the
   clear and smooth the ring instead; rejected, it hides the loss and still slows the finish.
3. **Dots for known stickers.** `ScanPaint` gets `dots` (key, centre, step vectors, colour) beside
   the veils: on found faces from the face's names where `recognised[n]`, on projection sides from
   `state.stickers`. Drawn as a circle of about 35 % of the sticker step, filled with the
   `StickerColors` display colour (as the 3D cube and the colour check use) with a thin dark rim so
   white and yellow show on a bright sticker. Dots glide with the same `Glide` keys as the veils and
   fade with the same alpha. Alternative: full tiles in the read colour (as before
   `scan-paint-calm`); rejected by the user's earlier "too busy".
4. **Moving threshold 1.0 side width a second.** `MOVING_SIDES_PER_SECOND` goes from 0.33 to 1.0;
   `REST_MILLIS` stays 300 ms. A hand-held cube jitters well under a side a second; turning it to
   another side is faster. Tuned on the phone by the user afterwards if needed.

## Risks / Trade-offs

- [A wrong pile named by elimination (e.g. a reflection on a face) would take the sixth colour] →
  only with five surely named piles, and the best-cube check still has to fit; a wrong name makes
  no cube fit and the stall notice comes as before.
- [Dots over a misread are back on screen] → that is the point (the user asked to see what the app
  read); they are small and centred so the real sticker shows around them.

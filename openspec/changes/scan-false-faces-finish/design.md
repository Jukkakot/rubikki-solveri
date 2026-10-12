# Design

## Context

**Finder (`FaceFinder`).** A face is a 3×3 lattice of sticker blobs around a centre blob: two step vectors from
the centre's neighbours, edge places predicted from the steps (`stretch` along, `tolerance` sideways), corners
from the edges found, sticker areas within 4× of the median, and `FaceLattice.span` at least `minSpan` 0.6 (a
neighbour pair must span 60 % of a step: a row of the next face is seen flatter). Recording `web_20261012_100233`
still has lattices over the cube's edge: `WWW|GRB|.RB`, `WWW|WWB|WRB` (white top row over a side), with steps
like u=(25.6,−3.1), v=(0.9,28.3) in one picture. On the striped cube every row of a side looks alike, so a lattice
shifted by one row fits as well as the true one. At least 56 of its 1488 pictures carry one (a whole white or yellow row beside other rows); their tracks read
pieces no cube has (`RWB RWB RWB`, `WBG WBG WBG`).

**Finish.** `VideoScan` finishes when the best cube is clear (`CLEAR_MARGIN`), quiet (`quietFor`) and
`FaceTracks.turnsClear()`: every other turn of two settled faces, and every side half round for three or more,
costs at least `FINISH_TURN_MARGIN` more. `turnsClear` only looks at faces whose turn the best cube settled
(`byCube`); faces settled by their look or by the picture's pair rules are not tried. 10:01:43 finished with
every side half round; `202156` (CENTRES) and `202058` (FACE_ONLY) did offline (`scan-warm-references` Findings),
and `web_181940` in fake-camera runs (`scan-send-ahead` Findings).

## Goals / Non-Goals

**Goals:** no lattice over a cube's edge taken for a face; no finish while a differently turned cube fits nearly
as well; both general, for every cube.

**Non-goals:** recognising the striped cube or any pattern; red/orange references (`scan-warm-references`);
finder speed.

## Decisions

1. **A face is flat: steps change gently.** For a candidate lattice, compare its two halves along each step: the
   distance from the middle row to each outer row (and the middle column to each outer column), and the outer
   rows' sticker sizes against the middle row's. Perspective on one plane changes them only a little and smoothly;
   over the cube's edge one outer row is foreshortened or turned, so its step or size jumps. Reject a lattice
   whose ratio of the two half-steps, or whose outer row's sticker size ratio, is beyond a limit. Limits are set
   from data: the largest ratios on true faces of every committed still (all sources, plain and striped) with a
   margin, checked against the lattices over an edge in the stills and the recording's readings. *Alternative:*
   the dark gap between the rows (a face has an even gap, an edge a wide dark band): needs pixel work in the
   finder; kept as a fallback if the step test does not separate them.
2. **The mirror finish: reproduce, then fix the cause.** First reproduce offline with the scan as it ships:
   `202156` with CENTRES references (finishes as its mirror), the fake-camera picture order of `web_181940`
   (frames dropped as a slower finder would), and `web_20261012_100233` replayed. Log for the finish moment which
   faces were settled how (`byCube`, look, pair rule) and what the half-round cube cost. Candidate causes and
   their general fixes, chosen by what the replay shows:
   - lattices over an edge gave false pair rules (fixed by decision 1);
   - `turnsClear` skips faces not settled by the cube: try every settled face's other turns, however settled;
   - the half-round cube is only told apart by pictures with two faces: require such a picture's evidence before
     a finish when the half-round cube is within `FINISH_TURN_MARGIN` (the spec scenario "Every side half round").
   If none reproduces, stop and report (the fix would be a guess).
3. **Go rules** (as in earlier spikes): no source finishes later by more than 10 % or wrong where it did not;
   every fixture and recording test passes unchanged; the work counters' budget test passes.

## Risks / Trade-offs

- [Rejecting a true face seen at a steep angle] → limits from all true faces in the stills plus a margin; the
  harness counts faces found per source before and after.
- [A stricter finish slows good scans] → the half-round check only bites when the half-round cube is close; go
  rule 3 checks every source.
- [The lost recording] → reproduction from other sources; if the cause cannot be shown, report instead of fixing.

## Migration Plan

Shared scan code; ships with the next app build and Pages deploy. Rollback: revert.

## Open Questions

- Whether the 10:01:43 finish came from lattices over an edge (decision 1) or from `turnsClear` (decision 2)
  cannot be known without its recording; the user is asked to share the log right after a wrong finish.

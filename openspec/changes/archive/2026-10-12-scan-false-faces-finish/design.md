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

## Findings

**1.1 Flatness of found lattices (no go).** `FalseFacesHarness` (FALSE_FACES=1) over every committed still at the
finder's size. A lattice counts as true when its reading fits a true face on at least 7 of 8 outer stickers, and
as over an edge when it fits fewer than 6 and some face agrees on its middle row and one outer row but not on the
other outer row. Per row and column of three stickers: the half-step jump |ln(d01/d12)|, the size jump
|ln(a0/a1) − ln(a1/a2)| and the bend (sine of the angle between the half-steps).

| source | lattices | fit no true face | over an edge | true faces at 99 %: steps, sizes, bend | over an edge, median |
|---|---|---|---|---|---|
| 151828 | 495 | 178 | 39 | 0.36 1.60 0.37 | 0.16 0.29 0.11 |
| 202156 | 818 | 215 | 38 | 0.46 1.72 0.40 | 0.21 0.72 0.24 |
| web_181940 | 1275 | 347 | 109 | 0.51 1.81 0.42 | 0.24 0.70 0.21 |
| web_084657:66 | 451 | 207 | 63 | 0.40 1.74 0.41 | 0.31 0.85 0.25 |
| web_121505:66 | 512 | 262 | 62 | 0.38 1.45 0.46 | 0.24 0.65 0.24 |

(All 15 sources alike.) Lattices over an edge are as even and straight as true faces, often more so: at 240 px
sticker areas and centres are noisy (true faces reach step jumps of 0.7 and size jumps of 3), while a lattice over
an edge seen from about 45° keeps its columns straight and its steps even (both faces foreshortened alike). On the
scrambled sources a step limit of 0.4 would lose 0.6 % of true faces and catch 18 % of the false ones. The
dark-gap fallback measures what `minSpan` already does (sticker extent over step). On the striped cube the colour
labels are unreliable too: random lattices over two faces fit a striped face 7 of 8. Recording
`web_20261012_100233` (readings only, no sticker geometry): 2198 readings, 916 fitting no true face, 119 read like
over an edge. **Decision 1 is not built**; the finder is unchanged (task 2.1 open).

**3.1 Mirror finish reproduced.** `202156` stills with CENTRES finished at 18.0 s with every side half round. At
the finish (`MirrorFinishHarness`, `VideoScan.finishDetail`): U settled by pictures, F, L, B turns settled by the
best cube, R and D open; the best cube cost 22.44 and the same evidence with every face half round 22.44, an exact
tie. Two causes, one behind the other:
- `turnsClear` tried only faces settled by the cube (F, L, B); those three half round without R cost more, so the
  mirror (all four sides) was never tried. Fixed: every face whose turn no picture fixed takes part, open ones too.
  Alone this moved the wrong finish to 46.5 s: meanwhile the assignment had settled R and L in the mirror's turn
  from the cube-settled F and B (counted as settled by a picture, out of `turnsClear`'s reach).
- `settleTurns` settled each face's turn when its own other turns cost `TURN_MARGIN` more, never trying faces
  turned together; each side's own half turn costs a lot, the mirror nothing. Fixed: faces that three or more
  turned half round together read within `TURN_MARGIN` stay open until a picture settles them.

With both, `202156` CENTRES finishes right at 15.7 s (`MirrorFinishTest`; fails on the old code). The 10:02
recording still never finishes (its false lattices, 1.1).

**4.1 Before / after** (`ColourSpikeHarness`, finished at; NEAREST is the shipped mode):

| source | NEAREST before → after | CENTRES before → after |
|---|---|---|
| 202156 | 20.5 → 17.9 s | 18.0 s WRONG → 15.7 s |
| 202403 | never → 13.3 s | never → 12.1 s |
| web_121505:66 | never → 29.8 s | never → never |
| rec 20261011_100403 | 11.7 → 11.1 s | 15.3 → 17.9 s |
| every other source | unchanged | unchanged |

No source finishes wrong; in the shipped mode none later. Scan time a picture rises by up to about 50 % on some
sources (more solves for the joint turn trials, e.g. `100824` 4.8 → 7.2 ms); the work counters' budget test and
every fixture and recording test pass. Go rules met for the shipped mode; CENTRES (not shipped) is 17 % later on
`100403`.

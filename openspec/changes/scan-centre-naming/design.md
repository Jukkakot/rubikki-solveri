# Design

## Context

See proposal.md (Why). Today in `VideoScan` a face reading joins the group of the colour its centre
is closest to (`nameCentres`: brightness-free distance to the default palette, or to the six
groups' mean centres once six exist; two faces of one picture never share a colour). Groups are
keyed by colour (`groups: Map<CubeColor, Group>`). `nameJointly` renames the groups together, each
colour once, only when all six exist and each has `MIN_VOTES` inliers.

What went wrong on 2026-10-07 14:55: the blue centre (in that light nearer white than blue against
the palette) started the "white" group; the real white face later joined it; the blue face's
readings were split between the "white" and "blue" groups frame by frame. Nothing could separate
them until all six groups were renamed together.

Measured on the 2026-10-07 camera video `20261007_132721` (same cube and light), brightness-free
distances to the palette: white centre `8faac8` W 23.6 / B 69.8; blue centres `2c4d6e` W 47.5 /
B 49.4, `284764` W 45.8 / B 52.1. The two faces' centres are far apart from each other even when
both are nearest white; the palette is what cannot tell them apart.

## Goals / Non-Goals

**Goals:** blue and white never share a pile; the right names as soon as the piles allow; no wrong
stickers shown as known while a name is in doubt; no test video slower or wrong.

**Non-Goals:** the paint (separate change `scan-paint-calm`); the finder; colour naming of the
stickers other than the centres.

## Decisions

1. **Piles by the cube's own centres.** Groups get their own identity (a list, not keyed by
   colour); each keeps the mean of its readings' centres (as `meanCentre` now). A full face joins
   the pile whose mean centre is closest in the same brightness-free distance, if within
   `JOIN_WITHIN`; otherwise it starts a new pile. A partial face only joins an existing pile within
   `JOIN_WITHIN`. The two-faces-in-one-picture rule (`scan-centre-clash`) carries over: faces of one
   picture are matched to piles closest first, each pile once; the loser takes its next pile within
   `JOIN_WITHIN`, else a new one. `JOIN_WITHIN` is measured on the fixtures: above the spread of one
   face's centres across a video, below the closest two different faces' centres (blue/white,
   red/orange). Alternative considered: keep naming by palette and only delay a doubtful face;
   rejected, the blue face's readings would still have nowhere to go.

2. **At most six piles.** When a seventh would start, the new centre joins its nearest pile, unless
   a pile has fewer than `MIN_VOTES` readings (a stray, such as a lattice across an edge): that one is
   dropped and the new pile takes its place. Two piles whose means come within `JOIN_WITHIN` (the
   light changed while one face was seen) are merged.

3. **Named together, always.** After the piles change, they are named together: the k ≤ 6 piles get
   distinct colours with the least summed distance to the palette (all assignments; at most 720),
   the current names winning a tie (`centreNamings` generalised to fewer than six). With five piles
   the sixth colour is the one left. A pile that changes name keeps its readings and votes; only its
   known stickers are worked out again (as `nameJointly` does now).

4. **Doubtful pile.** A pile is doubtful while the best naming that gives it a different colour costs
   less than `DOUBT_MARGIN` more than the best naming. A doubtful pile keeps collecting readings but
   gives no evidence to the best cube and none of its stickers is shown as known (its centre neither;
   the scan does not finish). Once it is clear, its votes count at once. `DOUBT_MARGIN` is measured on
   the fixtures: the 2026-10-07 blue centre alone (W 47.5 / B 49.4) is doubtful; once the white face
   is a pile of its own it is not; no face in the other videos stays doubtful for long.

5. **Regression fixture without new video.** The screen recording shows the app's paint over the cube
   from the third second, so the finder reads the paint. The test is built from `20261007_132721`:
   its frames whose only full face is the blue face, centre made pale as in `scan-centre-clash`'s test
   (named white by the palette), replayed first for about three seconds, then the whole video. A camera
   video (the phone's own camera app) starting with the blue face on top in the same light would be a
   better fixture: the user will film one (2026-10-07). If it is in `testdata/video/` when 1.1 starts, its finder readings become a fixture (`VideoScanHarness.writeFixtures`, 360×640 at 10 fps) and the test replays it as well; the built sequence stays either way.

## Risks / Trade-offs

- A face whose centre looks very different in two lights (window light vs shade) could start a
  second pile; the merge (2) catches it once its mean comes close, and the six-pile cap otherwise.
- Red and orange centres can sit close in dim warm light; if `JOIN_WITHIN` cannot separate them,
  they share a pile as some frames do now. The fixtures decide; if there is no clean gap, the
  threshold favours keeping today's behaviour for red/orange (a measured value per pair is not
  planned).
- A doubtful pile makes the scan wait for another face. Accepted: showing wrong stickers costs more
  (the 23-s and the given-up scan).

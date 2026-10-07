# Design

## Context

See proposal.md (Why). Today's chain (`VideoScan`): the finder gives full and partial face lattices
per frame (`FaceReading`: nine colours, centre, step vectors `u`, `v`); faces are piled by how their
centre looks (`pileFaces`), named jointly by centre colour (`nameJointly`), turned by neighbour
views (`updateRotations`), and the cube comes from the best-cube fit (`BestCube`). Faces seen
together already record which side they touch (`FaceReading.sideTowards`); a corner view is three
full faces that pairwise touch.

The fixtures (`cube/src/jvmTest/resources/video/`, true cubes in `VideoFixtures`) have plenty of
three-face frames: `20261007_132721` 185 of 425, `20261007_152753` 73 of 91, `20261007_132049` 57
of 134, `20261005_151828` 44 of 250; the straight-on and evening videos almost none.

## Goals / Non-Goals

**Goals:** a corner reader that names and turns three faces from one frame; numbers comparing
corner-anchored naming with today's scan on every fixture; a clear recommendation.

**Non-Goals:** any app or screen change; a new finder (the existing lattices are used as they are);
reading a face's stickers differently (voting and the best-cube fit stay).

## Decisions

1. **Corner = three full faces that pairwise touch.** For each pair `sideTowards` must give a side,
   and each face's two touching sides must be adjacent (they meet at one of its corner stickers).
   That corner sticker gives the face's rotation relative to the corner directly.
2. **Handedness from the picture.** Going round the three face centres about their mean, in screen
   order (y down, back camera not mirrored), gives the faces' clockwise order as seen from outside
   the cube. Each of the 8 corners of the colour scheme has a fixed clockwise colour order, so the
   hypotheses are 8 corners × 3 starting faces = 24. Each is scored by the three centres' distances
   to its colours (the palette's scaled Lab, as `centreDistances`); the best wins when it beats the
   next hypothesis by a margin, else the frame names nothing. White–green–red and
   white–green–orange are different hypotheses with opposite handedness, so red against orange is
   decided by which way round the faces run, as long as white and green are clear. Alternative:
   name the three centres freely and check handedness afterwards; rejected, it throws away the
   strongest constraint.
3. **Votes over frames.** Each corner reading votes for its faces' names and rotations; a pile
   (today's piling by look stays for faces seen alone) takes the name and rotation its corner votes
   agree on. A face never seen in a corner falls back to today's naming. This is the "hidden inside
   free scanning" shape, measured as such; the guided shape is judged from how often the fixtures
   show two opposite corners and how fast they finish when they do.
4. **Harness, not app.** A JVM harness replays each fixture through today's `VideoScan` and through
   the corner-anchored variant, and reports per fixture: frames to a finished cube, wrong cubes
   (finished but not the true one), share of frames with a corner, both opposite corners seen
   (yes/no and when). A **robustness run** repeats it with the red centre faded towards orange (as
   `scan-steady-progress`'s test) and the blue centre faded towards white (as `scan-centre-naming`'s),
   since those are the failures seen on the phone.
5. **The striped cube as a fixture.** Stills are cut from the 18:19 screen recording (the camera
   part of the screen, 10 fps, scaled to 360 px wide like the others) and run through the finder;
   true cube `WWWWWWWWWBRGBRGBRGOGROGROGRYYYYYYYYYGOBGOBGOBRBORBORBO`. The recording carries the
   scan's own marks over the picture; they are mostly hidden in that run (the cube moving), and
   frames with marks drawn are dropped if the finder trips on them.

## Risks / Trade-offs

- [A corner view's faces are foreshortened, and a lattice can slip a row (the white stickers read on
  the orange face at 0 s of the 18:19 run)] → a corner counts only when all three faces are full and
  the touching sides are consistent; a slipped lattice breaks the adjacency and is dropped.
- [Handedness flips if a picture is mirrored (front camera, a mirrored web stream)] → the harness
  checks the sign on fixtures with known cubes; if any source is mirrored, the scan must know it.
- [The fixtures were recorded for today's scan, not for corners] → the "both corners seen" numbers
  say how natural corner views are without guidance; a guided mode would get them on purpose.
- [A screen recording is not a camera video] → if the striped fixture is too poor, a camera video of
  the striped cube is asked from the user.

## Answers from the user (2026-10-07)

- **Shape:** hidden inside free scanning only. No guided "show this corner" mode and no asking the
  user to tell colours apart (`product.md`, "Scanning as easy as possible"). The harness still
  reports how often both corners are seen, as a measure of how far free scanning gets.
- **Striped fixture:** try the screen recording first (design 5).
- **Go bar:** never a wrong cube on any fixture (normal and robustness runs), and clearly fewer
  frames to finish than today's scan in the robustness run.

## Decisions while implementing (2026-10-07)

- **Corner test by the common point, not `sideTowards`:** in a corner view the top face's
  neighbours lie diagonally in its lattice, so `sideTowards` rejects them. The three centres' mean is
  where the cube's corner shows; each face's corner sticker points to it (1.0–3.0 steps out on both
  axes on the fixtures).
- **The check of a corner reading** is whether its side and turn fit the face's stickers best (named
  by the picture's own centres): single misread stickers and dim yellow reading green made a fixed
  "7 of 8" check call right corners wrong.
- **The experiment had to reach the piling** (`rules` in `pileFaces` and `nameJointly`): rules in the
  naming alone changed nothing (findings 3).

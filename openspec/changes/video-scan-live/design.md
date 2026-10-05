# Design

## Context

`video-scan` feeds only full faces (`FinderResult.faces`) into `VideoScan`; the finder also returns
faces with 7–8 stickers (`FinderResult.partial`, 223 on the angled test video) that are dropped.
The progress cube animates (450 ms slerp) to one of 24 poses whenever the discrete `Pose` changes,
so it jumps instead of following. Stickers are grey until recognised (≥ 3 agreeing votes). The
camera picture shows only cyan face outlines.

## Goals / Non-Goals

**Goals:** partial faces as input; a spike on smaller pieces with a clear build / drop criterion;
read colours drawn on the camera picture; a progress cube that follows the real cube every frame;
faint unconfirmed stickers.

**Non-Goals:** the video scan as default or a new way of choosing the scan (later change); reading
a cube while its layers are turned; tracking pieces from frame to frame by motion.

## Decisions

- **Partial faces (7–8 stickers).** A reading carries nine colours, missing ones null. It is
  grouped by its centre colour as now; a partial face without its centre is dropped (cannot be told
  apart safely). Its turn against the group is found on the stickers it has, and it votes only when
  all but at most one of them agree with the group. It is never a group's anchor, so a group starts
  only from a full face. It may give corner-view observations like a full face (the centre and the
  steps are known).
- **Smaller pieces: spike inside the change (task group 2).** Lower the finder's hit floor to 4–6
  stickers that include the centre, and measure on both test videos with the replay harness. Built
  in only if, on both videos: the replay's final cube is still the true one, no sticker is
  recognised wrong at any frame, and the cube is complete in fewer frames (or more stickers are
  recognised at the halfway frame). Otherwise the floor stays at 7 and the numbers go into
  `findings.md` of this change. Autopilot decides from the numbers; no stop.
- **Real-time orientation from the lattices (pure Kotlin in `cube`).** Weak-perspective model: a
  face's steps `u`, `v` are the screen projections of two cube axes (scaled). The third axis's
  projection follows from the two rows of a scaled rotation being orthogonal and of equal length
  (two equations, two unknowns; the sign from the face looking towards the camera), the depth row
  from the cross product. This gives a full 3D rotation from one face per frame, including tilt.
  Which cube axes `u` and `v` are comes from the face's identity and its settled rotation; faces
  whose rotation is not settled are not used. With several faces, the largest (most frontal)
  is used. `VideoScanState` gets the orientation (a rotation, or null when no usable face is in the
  frame).
- **Tilt sign (decided in apply).** The two weak-perspective answers are mirror images tilted
  towards or away from the camera, and both look at it, so the face's facing does not pick one.
  Instead: another face in the same frame (its centre lies where its normal says) picks it; without
  one, the answer closer to the last orientation; with no history, the first. Near straight-on the
  two answers coincide, and steep views usually show a second face. `VideoScanState.found` became
  `FoundFace` (reading, names, recognised flags).
- **Smoothing in the screen.** Frames come at ≤ 10 fps; the progress cube eases towards the latest
  orientation at display rate (exponential, about 150 ms to close most of the gap) along the
  shortest way, so it moves smoothly without jitter. No usable face → it stays put. The discrete
  `Pose` stays for the hints.
- **Faint stickers.** `VideoScanState` gets the leading colour per sticker (any vote, not yet
  recognised). The progress cube draws it mixed with the grey (about one third colour); recognised
  stickers full; contradictions marked as now. Before a face's rotation is settled its stickers
  are already drawn by the best guess (as for recognised ones today).
- **Colours on the camera picture.** For each face found in the frame, `VideoScanState.found` carries
  per sticker the colour it was named in this frame and whether that sticker is recognised (in the
  group's own frame, so it works before the face's rotation in the net is settled). The overlay
  draws a round dot of about half the sticker's step at each sticker, in the app's sticker colour
  for that name, with a thin dark rim; solid when recognised, about 45 % opaque when not. The cyan
  face outline stays, thinner and dimmed (about half opaque) so the dots lead (user, 2026-10-05).
  Fixed colours, not theme colours (drawn on the camera image).
- **Fixtures.** `VideoScanHarness.writeFixtures` also writes partial faces (and, if the spike says
  go, the smaller pieces) so the replay tests use them without the frames.

## Risks / Trade-offs

- Partial lattices are more often wrong than full ones → never anchors, must agree with the
  group; the replay test checks no wrong recognised sticker on either video.
- Weak perspective is crude for a close cube at a steep angle → only used for the look of the
  progress cube; the smoothing hides small errors.
- The orientation may jump a quarter turn when a face's rotation settles differently → rare, the
  easing makes it a short turn.

## Open Questions

User answers 2026-10-05 (mockups https://claude.ai/artifact/PoE7Ldoxa2VFt2TWKvYdoW): round dots;
dimmed outline; two levels on the progress cube (faint from the first reading, full when
recognised, grey only with no reading); full 3D following; follow only faces with a settled
rotation; a partial face without its centre is dropped; autopilot decides on the smaller pieces
by the criterion above, with the numbers in the summary.

None open.

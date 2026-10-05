# Design

## Context

See proposal.md – Why. Today each face found in a frame has one median RGB per sticker (per
channel, over the blob's pixels; `FaceFinder`). `VideoScan` names each sticker against the cube's
own centres (mean Lab of each centre group over the whole scan) and the group counts one vote for
the nearest colour. `BestCube` turns the votes into costs (−ln smoothed share, red/orange lending).
Exposure and white balance are locked on the first face, so the light's colour stays fairly steady
within a scan but differs between faces turned towards and away from a lamp. The test fixtures
(`cube/src/jvmTest/resources/video/`) hold the finder's colours per frame, regenerated from the
local frames (`VideoScanHarness.writeFixtures`); the true cubes are in `VideoFixtures`.

## Goals / Non-Goals

Goals: both dim evening videos finish with the true cube; no test video slower or wrong; no wrong
finish in the simulation. The stall notice never blocks scanning.

Non-goals: GPU, Web Worker, a learned classifier, camera settings beyond the existing locks.

## Decisions

1. **Measure first.** Before changing the reading, a harness prints for every video, from the
   fixtures and the true cube, how far each true red and orange reading lies from the red and the
   orange reference, raw and after each step below. Each step is kept only if it separates them
   better on the dim videos without hurting the good ones; the numbers go into `findings.md`.
2. **Light correction per frame (von Kries).** The stickers in a frame the scan takes as white
   (known or leading white; before anything is known, the brightest near-grey stickers) give the
   light's colour; every reading of that frame is scaled per channel so that white comes out grey,
   at the brightness it had. Without a white sticker in view, the last frame's correction holds.
   The centre references are built from corrected readings too.
3. **Soft votes.** A reading gives each colour a share by how close it is (a Gaussian of the Lab
   distance to that colour's reference, shares summing to 1), so a reading halfway between red and
   orange gives each about half. Votes become fractional everywhere (group counts, `StickerEvidence`,
   the vote-sure rule compares sums). The width of the Gaussian comes from the spread of readings
   around their true colour in the good-light videos (measured in 1). Red/orange lending stays.
4. **Glare.** A sticker's colour is the median of its pixels after dropping the brightest washed-out
   ones (low saturation and much brighter than the blob's median); white stickers keep theirs (all
   their pixels are near grey). Fixtures are regenerated, so all video tests run on the new colours.
5. **Threshold re-checked.** The simulation draws soft votes (a misread gives a share to the wrong
   colour, a borderline reading splits); T is re-chosen by the same rule if the sweep moves.
6. **Notice instead of panel.** A card at the bottom of the camera picture: reason icon and words,
   small "Aloita alusta" and "Korjaa värit" buttons, and "Sytytä valo" when the device has a torch,
   it is off and the reason is darkness or no progress. A tap on the picture outside the card closes
   it; it does not come back for the same stall, only for a new one (after progress, or a different
   reason). It goes when new stickers become known (the stall clears, as today).

## Risks / Trade-offs

- White may be wrong early (a yellow sticker in dim light can look whitish); the correction is
  limited to a plausible range and the measure in 1 shows whether it helps.
- Soft votes make clear cubes need a few more frames in good light; the video tests watch the frames
  to clear.
- If the dim videos still do not finish, the findings say why and the change stops there (no further
  tricks inside this change).

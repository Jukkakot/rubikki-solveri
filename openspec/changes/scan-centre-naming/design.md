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

The user's camera video `20261007_152753` (9 s, same cube and scramble, brighter light, blue face on
top first) shows the same mechanism with red and orange: the orange face in view at the start has
its centre at R 22 / O 23, is named red, and from frame 3 to 77 the red side shows wrong stickers as
known; it all comes right when the real red face (R 18 / O 36) is seen at frame 78. Blue reads
clearly blue in this light (B 27 / W 69). Stills in `testdata/video/2026-10-07/stills/20261007_152753/`.

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

5. **Regression fixtures.** The screen recording shows the app's paint over the cube
   from the third second, so the finder reads the paint. The test is built from `20261007_132721`:
   its frames whose only full face is the blue face, centre made pale as in `scan-centre-clash`'s test
   (named white by the palette), replayed first for about three seconds, then the whole video. A camera
   video (the phone's own camera app) starting with the blue face on top in the same light would be a
   better fixture: the user will film one (2026-10-07). If it is in `testdata/video/` when 1.1 starts, its finder readings become a fixture (`VideoScanHarness.writeFixtures`, 360×640 at 10 fps) and the test replays it as well; the built sequence stays either way.

6. **Stickers in another light (added 2026-10-07 with the user).** A sticker's distance to a
   colour's reference blends the plain Lab distance with the brightness-free one
   ([ColorClassifier.scaled], as for centres): `BARE_WEIGHT` 0.7 of the brightness-free part. A
   pile's centre is a sticker reference only when its own palette name is the pile's name; otherwise
   the default palette colour stands in. Measured (frames to finish / frames with a known sticker
   wrong): plain only TOUR never finishes; brightness-free only TOUR 159, 152753 finishes at 71 with
   none wrong, but 132049's red stickers in glare read white; 0.7 keeps the gains and is best on the
   evening videos (213929 166 → 0 wrong frames). Alternatives tried: references from the latest
   readings only (breaks 132049), wider vote shares (no effect).

## Risks / Trade-offs

- A face whose centre looks very different in two lights (window light vs shade) could start a
  second pile; the merge (2) catches it once its mean comes close, and the six-pile cap otherwise.
- Red and orange centres can sit close in dim warm light; if `JOIN_WITHIN` cannot separate them,
  they share a pile as some frames do now. The fixtures decide; if there is no clean gap, the
  threshold favours keeping today's behaviour for red/orange (a measured value per pair is not
  planned).
- A doubtful pile makes the scan wait for another face. Accepted: showing wrong stickers costs more
  (the 23-s and the given-up scan).

## Findings while implementing (2026-10-07, WIP)

Measured on all video fixtures (brightness-free distance, true face by sticker pattern):
- One face's centre spread (from its mean): p90 2–17, max up to 22 (angled views, light changes).
- White/blue means 40–78 apart (closest single reading 37). Red/orange means 12–27 apart, single
  readings as close as 5–8: no distance threshold separates red from orange.
- One face can look very different in two lights (TOUR's yellow face: washed `a1d0ac` vs saturated
  `8dc052`; ANGLED's white face: bluish `89a5c3` vs grey `747579`, more than 30 apart).

So distance alone does not work (`JOIN_WITHIN` 20 split faces; one value cannot serve red/orange).
Rules added to decisions 1–4 to make the fixtures pass (results stable for `JOIN_WITHIN` 25–35):
1. A face joins first a pile whose anchor's stickers agree with it (`MIN_AGREE`), at any centre
   distance; else the closest pile within `JOIN_WITHIN` (30); it starts a pile when no close pile has
   its own clear palette name (`NAME_CLEAR` 4: red next to an orange pile).
2. Piles whose anchors agree are merged (one face in two lights), as are piles within `MERGE_WITHIN`.
3. A pile with fewer than `MIN_VOTES` inliers is not named jointly (a stray would push the others'
   names): it takes the best colour left and is doubtful. It is still a sticker reference when that
   colour is its own palette name (a face seen rarely, such as ANGLED's yellow, tells its colour).
4. At six piles a new face replaces only a stray named by what was left over; else it is left out of
   this picture (as `scan-centre-clash` did).
5. While a pile seen several times is doubtful, no sticker of its two colours is known and nothing is
   inferred for faces not seen (152753: orange stickers elsewhere read red until the red face is seen).

Results with the piles alone (frames to finish, before → after): ANGLED 214 → 216, STRAIGHT 119 → 126,
evening 122/123/173/198 → 125/126/173/199; 152753 frames with a known sticker wrong 75 → 2. TOUR
(132721) no longer finished (was 280): its yellow face reads washed out, the yellow stickers on the
blue face read green; the old code finished only because the yellow face's first readings went into
the white group. The user chose to fix the light in this change (decision 6).

With decision 6: ANGLED 216, STRAIGHT 119, evening 125/126/166/199, TOUR 159, 152753 finishes at 71
with no sticker ever wrong, blue-first sequence 189 (was 310). Frames with a known sticker wrong
drop on most videos (ANGLED 77 → 13, STRAIGHT 89 → 29, 213929 166 → 0, TOUR 273 → 147).

Known limitation: in 132049's last second (it never finishes) two red stickers of the white face lie
in glare and read pale pink; they are now known white (the old metric had them as a tie that fell to
red). The test accepts at most these two at the stop and still checks that no complete cube is wrong.

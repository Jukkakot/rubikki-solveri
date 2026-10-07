# Design

## Context

See proposal.md (Why). All of it lives in `VideoScan` (cube module): readings are grouped by the
centre colour (`nameCentre`, brightness-free distances, the six groups' own centres once all are
seen), each group picks an anchor (the full reading most others agree with on ≥ `MIN_AGREE`), only
readings agreeing with the anchor vote, and a group keeps at most `MAX_READINGS` (40), dropping
non-agreeing ones first.

Evidence gathered while planning (temporary survey, not committed):
- 14 clean frames from the recording (2.25–2.5 s, before the paint appears), cropped to the camera
  picture and scaled to 360×640 like the 2026-10-05 stills, run through `FaceFinder`: three faces
  found right in every frame (red, white on top, blue on the right). Saved as the fixture
  `video/20261007_web.txt`.
- In those (screen-recorded, display-processed) frames the blue centre reads `#254d71`, which the
  scan names blue, so replaying them alone does not reproduce the bug; the phone's camera frames
  read it paler, and the log shows the scan named it white. The test therefore makes that centre
  pale (a colour the scan names white) to reproduce what the camera saw.
- The existing test videos have short runs of wrong readings (up to ~2 s); the scan survives them
  even when they are repeated 15 times, so they do not reproduce this case.

## Goals / Non-Goals

**Goals:** the 2026-10-07 case reads right; a face known wrong is put right by a few seconds of
clear views; nothing on the existing test videos gets slower or wrong.

**Non-Goals:** better colour naming in shadow in general (the naming itself is not changed);
changes to the finder; UI.

## Decisions

1. **One centre colour per face per picture.** Before the readings of a picture are added, each
   face's centre is ranked by distance to the six colours (the same references as now). If two
   faces of the picture name the same colour, the one closer to it keeps it; the other takes its
   next-best colour not taken in that picture (greedy, closest first; at most three faces are ever
   in view). Partial faces take part the same way (they still join only an existing group).
   Alternative considered: a sanity check on the colours inside the face (a "white" face full of
   yellow); rejected as fragile, while two faces in view are the common corner view.

2. **Straight-on weight.** A reading's squareness `s = |u×v| / (|u|·|v|) · min(|u|,|v|) / max(|u|,|v|)`
   (1 for a face seen straight on, smaller when sheared or foreshortened). Weight
   `w = clamp(s / 0.85, 0.3, 1)²`: moderate angles keep full weight, so recognition does not slow
   down (the survey: straight-on readings have median s ≈ 0.97, angled ones ≈ 0.82, steep ones
   0.4–0.6). `w` multiplies the reading's vote shares (on top of the washed-out weight) and its
   support for the anchor. Constants in `VideoScan`, tuned on the fixtures.

3. **Anchor by weighted support.** A full reading's support is the summed weight of the readings
   agreeing with it (itself included); the anchor is the one with the most, the current anchor
   winning a tie (as now). This lets a newer, clearer group take over once it outweighs the old one.

4. **Readings window: oldest out.** Over `MAX_READINGS` the oldest reading goes (never the one just
   added), whether it agrees with the anchor or not. At the phone's 15–30 readings a second this
   remembers the last 1.5–3 s of a face in view, enough votes for every sticker; old wrong readings
   age out. Dropping disagreeing ones first (today) is what threw the right readings away.

5. **Known stickers follow the votes.** No change needed beyond 3–4: a sticker keeps its colour
   (`sticky`) only while that colour still has at least the leading votes, so once the new group
   leads, the stickers change. A single wrong frame still cannot: it never outweighs the window.

## Risks / Trade-offs

- A face held at a steep angle for a long time and never seen better reads more slowly (weight
  0.3–0.5). Accepted: the user turns the cube; the regression videos check the speed.
- The window forgets: a face seen briefly long ago keeps its votes (only the oldest of a full
  window go), but a face stared at with a sustained misread can flip to the misread. Same as today
  in effect, and the centre rule removes the case seen.
- The centre rule picks wrongly if the face that truly owns the colour is the one with the paler
  match; then the readings swap groups for that picture. Two faces of one picture are always
  different, so at worst one picture's readings go to the wrong face, which the votes outweigh.

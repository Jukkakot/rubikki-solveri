# Proposal

## Why

The fast method's mirror is a separate small cube on a card in the corner: it reads as a second
cube, not as a mirror, and the user has to work out how it relates to the main cube. The user
wants a real mirror in the 3D scene behind the guide cube, showing a true reflection of the back
(decided 2026-10-04).

## What Changes

- The mirror card (small flipped cube with the label "Peili") is removed.
- The guide cube's 3D scene gets a framed mirror behind the cube, up and to the left of it,
  turned so the camera sees the cube's reflection in its middle. The reflection is computed, not
  faked: the cube is reflected in the mirror's plane, so it shows the back, left and bottom as a
  real mirror would, with the same highlight, arrow and turning animation.
- The mirror stays in place on the screen like a mirror on a wall: dragging turns only the cube,
  and the reflection follows it. No label (the frame makes it a mirror).
- The cube is drawn a little smaller so the mirror fits in the same box; the box's size rules
  (fit-screen) are unchanged.
- **One view everywhere** (user, 2026-10-04): the learn method's guide no longer swings round to
  the turning side; it keeps the steady holding view like the fast method, with the mirror and the
  reset button. Camera follow in the learn method gets the steady small cube (no mirror).
- **One vocabulary everywhere** (user, 2026-10-04): moves are described with the holding-view
  words in both methods, camera follow and the lessons' algorithm demo ("Käännä yläkerrosta
  oikealle", no more "…vastapäivään (katsottuna ylhäältä)"); step notes, lesson texts and tips
  use the same terms (yläkerros/alakerros, oikea/vasen/etu/takapuoli, ylöspäin/alaspäin).

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `move-guide`: the Mirror requirement changes from a separate mirror cube to a framed mirror in
  the scene with a true reflection; steady view, mirror and reset cover both methods; "View
  follows the move" is removed.
- `fast-solve`: moves in words are the same everywhere; other texts use the same terms.

## Impact

- `shared/.../ui/cube3d/CubeScene.kt` (mirror geometry, reflection, projection that fits cube and
  mirror), `ui/cube3d/Cube3D.kt` (draws the mirror, clipped reflection, frame),
  `ui/guide/MoveGuide.kt` (mirror card removed, `steady` flag gone), `ui/guide/FollowPanel.kt`,
  `ui/solve/SolveScreen.kt`, `ui/common/MoveWords.kt` (one wording), `ui/lessons/LessonScreens.kt`,
  strings in both languages (`mirror_label`, `move_cw`/`move_ccw`, `from_*` removed; texts with
  layer or direction words reworded).
- Tests: scene math on the JVM (`CubeSceneTest`), the guide's and words' existing tests
  (`SteadyGuideTest`, `SteadyWordsTest`, `MoveWordsTest`, `BeginnerTextsTest`), screenshots
  `solve`, `solve-learn`, `guide-back`, `guide-dragged`, lesson pages.

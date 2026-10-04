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
- The mirror belongs to the scene: when the user drags the cube, the mirror turns with it and the
  reflection stays true; when the mirror's glass faces away from the viewer it is not drawn.
- The cube is drawn a little smaller so the mirror fits in the same box; the box's size rules
  (fit-screen) are unchanged.
- Only in the fast method's guide, as today; the learn method, camera follow and lessons are
  unchanged.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `move-guide`: the Mirror requirement changes from a separate mirror cube to a framed mirror in
  the scene with a true reflection.

## Impact

- `shared/.../ui/cube3d/CubeScene.kt` (mirror geometry, reflection, projection that fits cube and
  mirror), `ui/cube3d/Cube3D.kt` (draws the mirror, clipped reflection, frame),
  `ui/guide/MoveGuide.kt` (mirror card removed), string `mirror_label` removed if unused.
- Tests: scene math on the JVM (`CubeSceneTest`), the guide's existing tests, screenshots
  `solve`, `guide-back`, `guide-dragged`.

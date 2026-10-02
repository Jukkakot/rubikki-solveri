# Design

## Context

product.md: 3D view drawn by the app with Compose (own projection), no 3D library. The move guide
(`move-guide`) and solution stepper (`fast-solve`) will reuse this view, so it takes plain inputs:
sticker colours, an optional move in progress with its progress 0..1, a view orientation and
marked stickers.

## Goals / Non-Goals

**Goals:** correct and smooth at 60 fps on the S24; math testable on the JVM; one composable
reused by every screen.

**Non-Goals:** turning layers by swiping (moves come from buttons and solvers), lighting/shading
beyond a simple face shade, the arrows and layer highlight of the move guide (`move-guide`).

## Decisions

- **Geometry:** 26 visible cubies of size 1 centred on −1..1 grid points; each draws its dark body
  (six quads) and its stickers (inset quads 0.84 wide, lifted 0.01 off the body). During a move the
  turned cubies' points are rotated about the move axis by −angle (clockwise from the axis tip).
- **Projection:** view rotation as a unit quaternion; perspective camera on +z at distance 9 with
  the cube fitting ~80 % of the canvas. Back-face culling per quad (normal vs. direction to the
  camera), painter's order by cubie centre distance from the camera, far first; within a cubie,
  culling makes order irrelevant (convex). Simple shading: faces turned away from the light are a
  little darker so the three visible faces read as 3D.
- **Pure Kotlin core** (`CubeScene`: quads, projection, culling, sorting, hit test) with no Android
  imports, unit-tested; a thin `Cube3D` composable draws the projected quads with `drawPath`.
- **Animation:** `CubeAnimator` (a Compose state holder) owns the shown cube, a move queue and an
  `Animatable` progress; quarter 300 ms, half 450 ms, ease in-out; a rotation (x y z) animates the
  whole cube. Duration is scaled by the system animator scale and is instant at 0.
- **View turning:** horizontal drag rotates about the screen's vertical axis, vertical drag about
  the horizontal axis (π radians per cube width). Programmatic orientation changes (the manual
  input's face preview) slerp to the target.
- **Hold orientations:** the manual input (and later the scanner) use the same per-face hold as
  the standard net: F, R (y), B (y2), L (y'), U (x', blue on top), D (x, green on top). These are
  in the `cube` module as `FaceView` so the scanner can reuse them.
- **Sticker colours** on screen are fixed, not themed (they must match the real cube): white
  #F4F4F4, yellow #FFD500, green #009E60, blue #0051BA, red #C41E3A, orange #FF5800, unknown grey
  #8A8A8A, plastic #141414.
- **Editor state** (`CubeEditor`, plain Kotlin, in `cube`): 54 nullable colours with centres fixed,
  counts, `isComplete`, `toCube()`. Saved across rotation as a string.
- **Validity messages** map each `Validity` reason to one localized sentence; marked stickers come
  from the reason.
- **Free cube** keeps an undo stack of moves; scramble plays a 20-move scramble at 2× speed.

## Risks / Trade-offs

- [Painter's order glitches during a slice turn] → sort by cubie centre after the move rotation;
  cubies are equal-sized and convex, so this is correct for a 3×3 grid in practice. Checked on the
  phone.
- [Canvas performance] → ≤ 216 quads per frame, paths reused; fine on the S24, listed for checking.

## Decisions made while building

- Until `fast-solve` exists, a valid manually entered cube opens in the free cube screen.
- Grid cells carry the colour as a state description (accessibility, and tests read it).
- Robolectric native graphics renders screenshots (`ScreenshotTest`) so the 3D view can be looked
  at without a phone; shading softened (0.8–1.0) after looking at them.

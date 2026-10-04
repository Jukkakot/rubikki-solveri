## Context

`Cube3D` draws the cube itself on a Canvas: `CubeScene.quads` gives the 26 cubies' quads in world
space, `CubeScene.project` rotates them by the view, drops back faces, sorts far to near and
projects with a fixed perspective (`CAMERA_DISTANCE` 9, scale fitted to the cube). The fast
method's mirror today is a second `Cube3D` with `MIRROR_VIEW`, flipped with `scaleX = -1`, on a
card in the corner of `GuideCube` (steady-cube, design decision 2).

## Goals / Non-Goals

**Goals:** a mirror that looks like part of the scene; a geometrically true reflection; the same
highlight, arrow and animation in it; works on Android and in the browser (pure Compose Canvas).

**Non-Goals:** lighting effects or blur on the glass; mirrors in camera follow or lessons;
reflections of the mirror in itself.

## Decisions

### 1. Mirror in camera space, placed by one rule

The mirror is fixed to the camera, not to the cube (user, 2026-10-04: a mirror on the wall stays
put when you turn the cube in your hands). It is a rectangle in camera space with centre `M`
behind the cube, up and to the left on screen (user accepted this default; may be tuned). Its
normal is chosen so that the ray from the camera to `M` reflects to the cube's centre:
`n = normalize(normalize(C − M) + normalize(O − M))` (`C` the camera, `O` the cube's centre). The
size is about 1.6 × the cube's width. `M`, size and frame width are constants (`MIRROR_*`) to tune
on the phone. The glass always faces the camera, so it is always drawn.

### 2. Reflection = the cube's quads reflected in the plane

The cube's quads are first rotated by the view (as today), then reflected in the camera-space
plane: `reflect(p) = p − 2((p − M)·n) n` for corners, `reflect(normal)` for normals (and the
arrow's points). Reflection flips handedness, so the reflected quads' corner order is reversed to
keep the back-face test right. The reflected quads go through the same projection (same highlight
and dimming), so turning, highlight and arrow come for free; dragging the cube changes the
reflection, never the mirror.

### 3. Drawing order and clipping

Per frame: (1) draw the frame (back plate), then the glass tint, then the reflected cube and arrow
clipped to the projected glass quad (`clipPath`); (2) draw the real cube over it. The mirror is
always behind the cube in depth, so drawing it first is enough; no shared depth sort.

### 4. Fitting the box

`scaleFor` today fits the cube alone. With a mirror, the projection fits the cube and the
mirror's corners (one fixed scale and centre offset; the mirror never moves, so dragging does not
zoom). The cube shrinks to about 75 % of today's width (user accepted, 2026-10-04); the box's size
rules (fit-screen) are unchanged.

### 5. Look

Frame: a rounded border in the theme's `secondary` colour (Karkki; no wood or metal, user
2026-10-04), glass: a light blue-grey tint in both themes so it reads as glass against the
background, reflected stickers slightly darkened (multiply ~0.85) so the reflection is told apart
from the real cube. No label (user, 2026-10-04).

### 6. API

`Cube3D(..., mirror: Boolean = false)`; `GuideCube` passes `mirror` to the main cube and drops the
second `Cube3D`, the card and the 10 % shift. `MIRROR_VIEW` and `mirror_label` are removed.

## Risks / Trade-offs

- [The cube gets smaller] → placement tuned for the smallest loss; constants tunable after a try.
- [Perspective makes the reflection small] → mirror close behind the cube; tune `M` by eye.
- [Cost: twice the quads per frame] → ~2 × 26 cubies, fine on phones and in the browser.

## Context

`Cube3D` draws the cube itself on a Canvas: `CubeScene.quads` gives the 26 cubies' quads in world
space, `CubeScene.project` rotates them by the view, drops back faces, sorts far to near and
projects with a fixed perspective (`CAMERA_DISTANCE` 9, scale fitted to the cube). The fast
method's mirror today is a second `Cube3D` with `MIRROR_VIEW`, flipped with `scaleX = -1`, on a
card in the corner of `GuideCube` (steady-cube, design decision 2).

## Goals / Non-Goals

**Goals:** a mirror that looks like part of the scene; a geometrically true reflection; the same
highlight, arrow and animation in it; works on Android and in the browser (pure Compose Canvas).

**Non-Goals:** lighting effects or blur on the glass; mirrors in the learn method, camera follow
or lessons; reflections of the mirror in itself.

## Decisions

### 1. Mirror in world space, placed by one rule

The mirror is a rectangle in world space with centre `M` behind the cube, up and to the left as
seen in the holding view (`DEFAULT_VIEW`). Its normal is chosen so that, in the holding view, the
ray from the camera to `M` reflects to the cube's centre: `n = normalize(normalize(C − M) +
normalize(O − M))` (`C` the camera in world space for `DEFAULT_VIEW`, `O` the origin). The size
is about 1.6 × the cube's width. `M`, size and frame width are constants (`MIRROR_*`) to tune on
the phone. Fixed in world space, so a drag turns it with the cube.

### 2. Reflection = the cube's quads reflected in the plane

`reflect(p) = p − 2((p − M)·n) n` for corners, `reflect(normal)` for normals (and the arrow's
points). Reflection flips handedness, so the reflected quads' corner order is reversed to keep the
back-face test right. The reflected quads go through the same `project` (same highlight and
dimming), so turning, highlight and arrow come for free.

### 3. Drawing order and clipping

Per frame: (1) if the glass faces the camera (`n` after the view rotation has a positive z
towards the camera), draw the frame (back plate), then the glass tint, then the reflected cube
and arrow clipped to the projected glass quad (`clipPath`); (2) draw the real cube over it. The
mirror is always behind the cube in depth, so drawing it first is enough; no shared depth sort.
When the glass faces away, nothing of the mirror is drawn.

### 4. Fitting the box

`scaleFor` today fits the cube alone. With a mirror, the projection fits the cube and the
mirror's corners in the holding view (one fixed scale and centre offset computed for
`DEFAULT_VIEW`, so dragging does not zoom). The cube shrinks to make room; the mirror placement
constants are chosen so it shrinks as little as possible (target: the cube at least ~75 % of
today's width).

### 5. Look

Frame: a rounded candy-coloured border in the theme's `secondary` colour (Karkki), glass: a light
blue-grey tint in both themes so it reads as glass against the background, reflected stickers
slightly darkened (multiply ~0.85) so the reflection is told apart from the real cube. No label.

### 6. API

`Cube3D(..., mirror: Boolean = false)`; `GuideCube` passes `mirror` to the main cube and drops the
second `Cube3D`, the card and the 10 % shift. `MIRROR_VIEW` and `mirror_label` are removed.

## Risks / Trade-offs

- [The cube gets smaller] → placement tuned for the smallest loss; constants tunable after a try.
- [Perspective makes the reflection small] → mirror close behind the cube; tune `M` by eye.
- [Cost: twice the quads per frame] → ~2 × 26 cubies, fine on phones and in the browser.

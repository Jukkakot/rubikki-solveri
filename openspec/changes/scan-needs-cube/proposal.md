# Proposal

## Why

The automatic capture takes pictures that are not a cube face: a room with the cube small in a
corner, a yellow-patterned blanket (recognised as the bottom by its yellow middle), a cube held off
the grid or so close that the grid covers one or two stickers. All of them were captured
automatically in one session (2026-10-04, Samsung Internet), and the scan ended in an impossible
cube. Today's check (`FrameSampler.looksLikeCube`: six of nine cells with a darker edge) lets all of
these through.

## What Changes

- **Every cell must look like a sticker** before the automatic capture runs: its middle is one even
  colour, and that colour is a cube colour (clearly coloured, or light and nearly grey for white).
  Dark, grey, beige and brown cells, and cells that straddle a gap or show a pattern, fail. The
  dark-gap check stays.
- **Live feedback in the grid:** each cell's outline shows whether that cell looks like a sticker
  (green when it does), so the user sees which part of the cube is off the grid. The status line
  says "Tuo kuutio ruudukkoon" while a cell fails.
- The capture button still captures whatever is in the grid (decided with the user).
- **Camera follow** uses the same check: frames where the grid does not show a face are ignored
  (no learning, no advance) and the screen asks to bring the cube into the grid.
- Also fixed: the scan treats "no grid picture yet" as a cube (`?: true` in `ScanScreen`); it
  becomes "not yet".

Not in scope: changing the hold time, the colour classification, landscape.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `camera-scan`: "No cube in the grid" requires every cell to look like a sticker, with per-cell
  feedback.
- `camera-follow`: frames without a face in the grid are ignored.

## Impact

- `cube/.../scan/FrameSampler.kt` (per-cell check), `ScanSession` (unchanged API: `looksLikeCube`),
  `cube/.../follow/FollowTracker.kt` (ignores frames without a face).
- `shared/.../ui/scan/ScanScreen.kt` (cell outlines, status, the `?: true` default),
  `ui/guide/FollowPanel.kt` and `SolveScreen.DefaultFollowPanel` (pictures to the follow panel),
  strings in both languages.
- Tests in `cube`: per-cell check against readings from the user's logs (numbers only, no pictures
  in the repo — decided with the user).

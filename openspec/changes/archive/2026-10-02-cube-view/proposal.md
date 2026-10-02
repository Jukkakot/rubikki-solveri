# Proposal

## Why

The app's core promise is that every move is unmistakable, and that needs a 3D cube on screen.
Manual input is also the first way to get a real cube into the app: it works before the camera
does and stays as the fallback when a scan fails.

## What Changes

- A 3D cube drawn by the app (own projection on a Compose canvas): real sticker colours, dark
  plastic between stickers, perspective, smooth animation of any move (faces, slices, wide turns,
  rotations), drag to turn the view, tap to find a sticker, highlighted stickers.
- Manual input screen: paint the cube face by face in a big 3×3 grid with a colour palette, with a
  hint how to hold the cube for each face; a mini map of all six faces; a 3D preview that turns to
  the face being edited; live colour counts; a check that names what is wrong and marks the
  stickers concerned.
- Free cube screen: the 3D cube with buttons for the six face turns (clockwise/counter-clockwise),
  scramble, undo and reset — to try the 3D view and learn the moves.
- Home: manual input and free cube are enabled.

## Capabilities

### New Capabilities
- `cube-view`: how the 3D cube looks, animates and responds to touch.
- `manual-input`: entering a real cube's colours by hand and checking them.

### Modified Capabilities
- `app-shell`: the home screen gains the free cube entry; manual input becomes available.

## Impact

- `app` module: new `ui/cube3d` (projection math in plain Kotlin + Compose drawing), screens
  `ManualInputScreen`, `FreeCubeScreen`, new strings. Uses `cube` for state, moves and validity.

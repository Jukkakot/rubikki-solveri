# Proposal

## Why

User feedback 2026-10-06 on the video scan:
- The marks on the cube lag behind the picture and flicker.
- The turning arrow comes and goes and its meaning is unclear.
- The six balls under the picture mislead: the scan can finish before any of them lights, because a
  side is ticked only when the rest of the cube confirms it while the scan finishes on the most
  likely cube.
- The screen has too much text and too many buttons.

The user wants the status visible on the cube itself: the real cube painted in its colours as it is
read, the paint never vanishing, so the grey parts tell what to show next (mockup A, "Maalaus
oikeaan kuutioon", chosen 2026-10-06).

## What Changes

- **Paint on the real cube:** every known sticker of the sides facing the camera is painted as a
  bright tile over the real sticker (smaller than it, so the real colour stays visible around it).
  A sticker still needed is a grey dashed tile. A side whose nine stickers the cube confirms gets a
  white outline instead of a tick.
- **Paint that stays:** the paint follows the cube smoothly between pictures (no jumps). It stays in
  place through short gaps when the cube's pose is not read, instead of vanishing.
- **Faster reading:** every picture the camera delivers is read (no own limit; about 30 a second on
  most phones, up to 60). The finder drops pictures it cannot keep up with, so nothing piles up.
- **Arrow removed:** the grey paint shows where to turn. **BREAKING** for the spec's turning hints.
- **Ball row removed:** a small ring at the top of the picture fills with the share of stickers
  known and is full when the scan finishes.
- **Cleaner screen:**
  - The camera fills the screen.
  - Back, torch and a ⋮ menu are round icons on the picture. The menu holds "Kuva kerrallaan",
    "Syötä käsin" and "Korjaa värit".
  - No title, no bottom buttons.
  - One short status pill at the bottom of the picture: "Näytä kuutio kameralle", "Näytä harmaat
    kohdat" or "Valmis!".
  - The dim-light and stall notices stay as they are.

## Capabilities

### New Capabilities

### Modified Capabilities
- `video-scan`: how the progress is shown changes. Other requirements change too:
  - Paint tiles replace dots, and the paint persists and moves smoothly.
  - The turning arrow is removed.
  - A ring replaces the row of six balls.
  - Every camera picture is read.
  - The switch to the guided scan moves into a menu.
  - A new screen layout requirement is added.

## Impact

- `shared`:
  - `ui/scan/VideoScanScreen.kt`: layout, paint drawing, smoothing, ring, menu. The arrow and the
    ball row are removed.
  - Strings in both languages.
- `cube`: the video scan keeps the last pose and projection, and anchors them to the faces found
  while the pose is not read (`cube/scan/VideoScan.kt`). Pure Kotlin with JVM tests.
- `app` (Android camera): the analysis throttle is removed (`CameraPreview.android.kt`).
- `web`: the browser camera hands every video frame to the worker instead of one per 66 ms.
- The log's snapshot already reports `fps` and finder time, so the effect on the phone can be read
  from the log.

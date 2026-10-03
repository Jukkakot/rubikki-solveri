## Context

`ScreenshotTest` (Robolectric native graphics, `fi-w411dp-h891dp-xxhdpi`) writes 15 PNGs to
`app/build/screenshots/`, a few in dark only. Home, settings, about, log, history, practice,
scramble guide and the permission screen are missing. Full-size PNGs are ~1233×2673, ~200 kB.

## Goals / Non-Goals

**Goals:** every screen, both themes, regenerated in one command; a gallery the user opens on the
phone or PC and comments on; near-zero upkeep.

**Non-Goals:** pixel comparison / regression failures (Roborazzi can come later); hand-written
descriptions; publishing anything outside the user's private claude.ai space.

## Decisions

- **`shot(name)` renders light and dark** (`name-light.png`, `name-dark.png`); the explicit dark
  variants go away. Doubles render time (~40 images, about a minute) — acceptable for a step run
  only after UI changes. Screens needing data (history, practice) get small fixed fakes in the test.
- **Gallery script** `scripts/screen-gallery.py`: groups images by a name prefix table (home,
  input, scan, solve, learn, progress, settings), downscales to 40 % JPEG with ImageMagick
  (~40 kB each, ~2 MB total), writes `build/gallery/index.html` with light/dark pairs and a
  generation stamp (commit and date). The page follows the artifact page contract (theme tokens,
  dark mode, phone width). No screen descriptions: the file name is the caption.
- **Publishing** is done by Claude with the Artifact tool (the page plus its images as files),
  always to the same URL, recorded in `docs/development.md`. Comments on the page reach Claude.
- **When to refresh:** after a pushed change that touches UI, as part of the change's last step,
  without a review stop. The user looks when they like.

## Risks / Trade-offs

- [A screen added later is forgotten] → the project instructions say a new screen gets a shot.
- [Robolectric renders differ a little from the phone (fonts, camera preview is black)] → the
  gallery is for layout and hierarchy; the real-phone check stays for the camera.

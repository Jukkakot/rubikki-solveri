## Why

The user steers the UI by looking at it, but the only way to see a screen today is to open it on
the phone. A gallery of every screen, in light and dark, generated from the code and viewable on
any device, lets them review the whole app at once and comment on specific screens. It must stay
cheap: generated, never drawn or described by hand.

## What Changes

- The screenshot test renders **every** screen (home, settings, about, log, manual input, check a
  scan, scan, scan review, camera permission, solve, guide, follow-along, free cube, lessons,
  lesson, practice, timer, history) in **light and dark**, at phone size.
- A script turns `app/build/screenshots/` into a gallery page (`build/gallery/index.html` plus
  downscaled images), grouped by area, light and dark side by side.
- The gallery is published once as a private claude.ai page; after a change that touches UI it is
  regenerated and republished to the same address. The address and the one-line refresh recipe go
  into the project instructions and `docs/development.md`.

No app behaviour changes (`skip_specs`). Modules: `app` tests only, plus a script.

## Capabilities

### New Capabilities

(none — developer tooling only)

### Modified Capabilities

(none)

## Impact

- `app/src/test/.../ui/ScreenshotTest.kt`: one `shot()` renders both themes; new shots.
- `scripts/screen-gallery.py` (new; uses ImageMagick, already on the dev machine).
- `.claude/CLAUDE.md`, `docs/development.md`: when and how to refresh the gallery.

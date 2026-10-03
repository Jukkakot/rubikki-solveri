## 1. Screenshots

- [x] 1.1 `shot()` renders light and dark; remove the separate dark shots; add home, settings, about, log, history, practice, scramble guide, camera permission; verify `./gradlew :app:testDebugUnitTest --tests '*ScreenshotTest*'` writes a light and a dark PNG for every screen

## 2. Gallery

- [x] 2.1 `scripts/screen-gallery.py`: group, downscale, write `build/gallery/index.html` (page contract: title, theme tokens light/dark, phone width); verify by running it and opening the page
- [x] 2.2 Publish the gallery as a private artifact; record its URL and the refresh recipe in `docs/development.md` and `.claude/CLAUDE.md` (refresh after UI changes, new screens get a shot); verify the URL opens

## 3. Docs

- [x] 3.1 Roadmap row 18 `screen-gallery` done; verify by reading

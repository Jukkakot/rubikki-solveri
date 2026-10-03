# Tasks

## 1. Theme

- [ ] 1.1 Add Fredoka and Nunito font files (SIL OFL) under `res/font`; typography with Fredoka for display/headline/title and Nunito for body/label
- [ ] 1.2 Raised shape scale; test fallback schemes from the `#1B6EF3` seed palette (dynamic colour stays on the phone)
- [ ] 1.3 Test: the theme provides the Fredoka/Nunito typography and the raised shapes in light and dark

## 2. Shared pieces

- [ ] 2.1 `RoundIconButton` and `BigButton` in `ui/common`, 48 dp / 64 dp, themed

## 3. Scan

- [ ] 3.1 Scan route (camera, permission, colour check) always dark via the forced dark theme
- [ ] 3.2 Scan screen after mockup A: round top-bar buttons, rounded camera view, face pips with count, round shutter (content description in both languages), review buttons as pills; still one screen without scrolling
- [ ] 3.3 Test: face pips show done/current/empty for two accepted faces; the shutter captures; existing scan tests stay green

## 4. Solve and the rest

- [ ] 4.1 Solve screen after mockup A: round back/icon buttons, thick rounded progress bar, "Näytä" pill, done as `BigButton`, instruction in the headline style; method choice kept
- [ ] 4.2 Replace UI hard-coded colours with scheme roles across the screens (keep sticker and camera-overlay colours)
- [ ] 4.3 List Fredoka and Nunito (SIL OFL) on the licences screen
- [ ] 4.4 Run the screenshot tests, look at scan, check, solve, home and settings in light and dark at phone size; fix clipping or contrast; republish the screen gallery (bigger UI overhaul)

## 5. Docs and roadmap

- [ ] 5.1 Update `openspec/context/product.md` (Look: Karkki shapes and fonts on Material You colours; scan always dark) and the theme note in `docs/architecture.md`
- [ ] 5.2 Mark roadmap item 23 `look-refresh` done

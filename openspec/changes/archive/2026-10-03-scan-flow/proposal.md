## Why

Phone testing showed that the scan blocks the user when its colour guesses are wrong: this cube's
red and orange read almost the same (`c4543f` vs `cf5744`), so the right face was refused as
"wrong side", and tapping stickers to fix them in the review was fiddly and taught the reading the
wrong lesson. The user knows best which face they hold; the app should guide, not stop. The colours
can only be told apart reliably from all 54 readings together, at the end.

## What Changes

- The centre check becomes a hint: a centre that reads as another colour than the asked face's is
  mentioned ("the centre looks red; if this is the right side, hold still") but the face is still
  captured.
- The only stop: while the camera still sees the face accepted last, the screen asks to turn the
  cube and captures nothing.
- A face counts as steady when the camera's readings stay close (by colour difference), not when
  the guessed colours stay the same.
- The live dots and the review show the colours as the camera sees them, not guessed colours.
- **BREAKING (UX):** tap-to-fix in the review is removed; the review offers "Good, next" and "Scan
  again" and says the colours are worked out at the end. Doubtful stickers are checked in the
  manual editor after the scan, as before.
- Records the home screen's version and install time line (already built in 41e2488) in the
  app-shell spec.

Modules: `cube` (scan session) and `app` (scan screen, strings). The home-screen line is already in
`app`.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `camera-scan`: live reading shows raw camera colours; centre mismatch is a hint, not a stop; a
  "previous face still in view" stop; steadiness by colour difference; review without tap-to-fix.
- `app-shell`: the home screen shows the app version and when this build was installed.

## Impact

- `cube/scan/ScanSession`: `WrongFace` event replaced by `PreviousFace`; `Holding` carries a centre
  hint; corrections and `cycle` removed; classification without user corrections.
- `app/ui/scan/ScanScreen`: raw-colour dots and review tiles, new status texts, no tile taps.
- Strings (fi/en), scan tests and the review screenshot.
- `docs/architecture.md` camera-scan section, `openspec/context/product.md` (tap to fix), roadmap.

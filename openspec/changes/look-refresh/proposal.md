# Proposal

## Why

The app still wears the stock Material You look: plain, with colours taken from the phone's
wallpaper, so it looks different on every phone and has no character. The user picked direction
**A "Karkki"** (soft, playful) from the mockups (2026-10-03,
https://claude.ai/artifact/3njsPL5v8qu8affuwk8x7z) and wants the look done before the home screen.

## What Changes

- **Own look "Karkki"** for the whole app, light and dark: warm cream ground in light, violet-black
  in dark, a blue primary and an orange accent, chunky rounded fonts (Fredoka for headings, Nunito
  for text), large rounded shapes and pill buttons. **BREAKING** for the theme spec: the phone's
  dynamic (wallpaper) colours are no longer used.
- **Scan screens always dark**, also when the app is light: camera, camera permission and colour
  check.
- **Scan screen restyled** after mockup A: round tonal top-bar buttons, the camera view with big
  rounded corners, six face pips showing which faces are done (filled with the face's centre
  colour) and which is next, a large round shutter as the capture button.
- **Solve screen restyled** after mockup A: big pill primary action ("Tein sen!"), round
  secondary buttons, a thick rounded progress bar, larger move instruction in the heading font.
- Every other screen gets the new look through the theme; hard-coded colours that clash with it
  are replaced by theme colours.
- The fonts' licence (SIL OFL) is listed with the other licences.

## Capabilities

### New Capabilities

### Modified Capabilities
- `app-shell`: Theme — the app's own Karkki look in light and dark instead of dynamic colours.
- `camera-scan`: the scan screens are always dark; capture is a round shutter; face progress as
  pips.

## Impact

App module only (theme, fonts, scan and solve screens, small colour fixes elsewhere, licences).
The cube module is unchanged. The 3D cube drawing and the move arrows keep their current look,
since move legibility is a core feature tuned on the phone.

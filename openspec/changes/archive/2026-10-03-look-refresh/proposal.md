# Proposal

## Why

The app wears the stock Material 3 look: default fonts and shapes, plain screens, no character.
The user picked direction **A "Karkki"** (soft, playful shapes and fonts) from the mockups, with
the phone's own Material You colours (2026-10-03,
https://claude.ai/artifact/3njsPL5v8qu8affuwk8x7z, rows A and "A + Material You"), and wants the
look done before the home screen.

## What Changes

- **Karkki shapes and fonts** for the whole app, light and dark: chunky rounded fonts (Fredoka for
  headings, Nunito for text), large rounded shapes and pill buttons. Colours stay the phone's
  Material You colours.
- **Scan screens always dark**, also when the app is light: camera, camera permission and colour
  check.
- **Scan screen restyled** after mockup A: round tonal top-bar buttons, the camera view with big
  rounded corners, six face pips showing which faces are done (filled with the face's centre
  colour) and which is next, a large round shutter as the capture button.
- **Solve screen restyled** after mockup A: big pill primary action, round secondary buttons, a
  thick rounded progress bar, larger move instruction in the heading font.
- Every other screen gets the new shapes and fonts through the theme; hard-coded colours that
  clash with the theme are replaced by theme colours.
- The fonts' licence (SIL OFL) is listed with the other licences.

## Capabilities

### New Capabilities

### Modified Capabilities
- `camera-scan`: the scan screens are always dark; capture is a round shutter; face progress as
  pips.

The app-shell Theme requirement (Material You colours, light and dark, follow the phone or forced)
already holds and is unchanged.

## Impact

App module only (theme typography and shapes, fonts, scan and solve screens, small colour fixes
elsewhere, licences). The cube module is unchanged. The 3D cube drawing and the move arrows keep
their current look, since move legibility is a core feature tuned on the phone.

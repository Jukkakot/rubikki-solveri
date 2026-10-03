# Design

## Context

`ui/theme/Theme.kt` uses Material You dynamic colours on the phone, with fallback schemes only for
tests. Fonts and shapes are Material defaults. The scan (`ui/scan/ScanScreen.kt`) and solve
(`ui/solve/SolveScreen.kt`) screens use stock `TopAppBar` and `Button`s. About 40 hard-coded
`Color(...)`/`Color.White` uses exist outside the 3D cube. The chosen look is mockup A,
https://claude.ai/artifact/3njsPL5v8qu8affuwk8x7z (the source of truth for the tokens below).

## Goals / Non-Goals

**Goals:** one theme that carries the Karkki look to every screen; the scan and solve screens
restyled after the mockup; scan always dark.

**Non-Goals:** the home screen (roadmap `home-design`, next); the 3D cube drawing and move arrows
(kept, legibility tuned on the phone); copy changes (no new texts beyond a content description);
navigation or behaviour changes.

## Decisions

- **Fixed palette, no dynamic colour** (user OK 2026-10-03). Light and dark `ColorScheme`s built
  from the mockup tokens: light ground `#FFF7EE`, text `#2A2233`, primary `#2F5FD6`; dark ground
  `#17151D`, text `#F6F1FF`, primary `#7FA8FF`; accent (tertiary) orange `#FF8A3D`; tonal chips
  `#F3E6D8` / `#2A2633`. The other roles are derived to keep M3 contrast (text 4.5:1). The
  `dynamicColor` parameter goes away; tests stop passing it.
- **Fonts bundled, not downloadable.** Fredoka (headings: display, headline, title styles) and
  Nunito (body and label styles) as `res/font` files under the SIL OFL. Bundling keeps the app
  offline and free of Google Play services; it adds a few hundred kB. Licences screen lists them.
- **Shapes:** M3 shape scale raised (small 12, medium 20, large 28, extra large 32 dp). Buttons
  stay pills (M3 default).
- **Scan is dark by wrapping, not by special colours:** the scan route (camera, permission, colour
  check) is wrapped in the theme forced to dark. Leaving the scan returns to the app theme.
- **Shared pieces in `ui/common`:** `RoundIconButton` (48 dp tonal circle for top-bar actions),
  `BigButton` (64 dp pill, heading font, the one main action of a screen). Scan and solve use
  them; other screens keep stock components themed by the scheme, so nothing is re-implemented
  per screen.
- **Shutter:** an 84 dp circle in the accent colour with a light ring; content description
  "Ota kuva / Take picture". During the review (accept / scan again) the two pill buttons stay.
- **Face pips:** six 34 dp rounded squares in a row, done ones filled with the centre colour,
  current one with a ring, count "n / 6" beside. "Scan one face" mode keeps its text.
- **Solve screen:** the method choice (shortest / learn) stays, themed; progress is a thick (8 dp)
  rounded bar; back is a round icon button, "Näytä" an outlined pill, done a `BigButton`. The
  instruction uses the headline style (Fredoka).
- **Hard-coded colours:** replaced by scheme roles where they style UI; kept where they are real
  cube or camera colours (stickers, overlays on the camera picture).

## Risks / Trade-offs

- Fixed colours lose the "matches my phone" feel → the user chose the mockup look explicitly.
- Rounder, larger buttons take height → the "no tall pages" rule still holds; checked with the
  screenshot tests at phone size.

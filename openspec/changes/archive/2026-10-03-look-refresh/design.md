# Design

## Context

`ui/theme/Theme.kt` uses Material You dynamic colours on the phone, with fallback schemes only for
tests. Fonts and shapes are Material defaults. The scan (`ui/scan/ScanScreen.kt`) and solve
(`ui/solve/SolveScreen.kt`) screens use stock `TopAppBar` and `Button`s. About 40 hard-coded
`Color(...)`/`Color.White` uses exist outside the 3D cube. The chosen look is mockup A with Material You colours,
https://claude.ai/artifact/3njsPL5v8qu8affuwk8x7z (rows A and "A + Material You").

## Goals / Non-Goals

**Goals:** one theme that carries the Karkki shapes and fonts to every screen; the scan and solve screens
restyled after the mockup; scan always dark.

**Non-Goals:** the home screen (roadmap `home-design`, next); the 3D cube drawing and move arrows
(kept, legibility tuned on the phone); copy changes (no new texts beyond a content description);
navigation or behaviour changes.

## Decisions

- **Colours stay Material You** (user choice 2026-10-03, after seeing the mockup with the colours
  a black wallpaper gives). Only typography and shapes change in the theme. The test fallback
  schemes switch to the palette Android derives from its fallback seed `#1B6EF3` (what the user's
  black wallpaper gives), so screenshots look like the phone.
- **Colour roles used by the new pieces:** tonal chips `surfaceContainerHigh`, the main action
  `primary`/`onPrimary`, the shutter `primary` with an `onSurface` ring. No fixed accent colour.
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
- **Shutter:** an 84 dp circle in `primary` with a light ring; content description
  "Ota kuva / Take picture". During the review (accept / scan again) the two pill buttons stay.
- **Face pips:** six 34 dp rounded squares in a row, done ones filled with the centre colour,
  current one with a ring, count "n / 6" beside. "Scan one face" mode keeps its text.
- **Solve screen:** the method choice (shortest / learn) stays, themed; progress is a thick (8 dp)
  rounded bar; back is a round icon button, "Näytä" an outlined pill, done a `BigButton`. The
  instruction uses the headline style (Fredoka).
- **Hard-coded colours:** replaced by scheme roles where they style UI; kept where they are real
  cube or camera colours (stickers, overlays on the camera picture).

## Risks / Trade-offs

- With a black wallpaper the colours are a cool grey-blue, less warm than mockup A → the user saw
  that variant and chose it.
- Rounder, larger buttons take height → the "no tall pages" rule still holds; checked with the
  screenshot tests at phone size.

## Decisions taken while building

- **Kept colours:** the timer's hold/ready red and green (the speedcubing signal convention) and
  the log's warning amber (M3 has no warning role) stay fixed, like sticker and camera colours.
- **Face pips:** done faces fill from the left in the order the screen knows (face order), not the
  scan order; the count keeps the "Kuvattu n/6" text. A one-face rescan shows its face name there.
- **Short labels by the shutter:** "Syötä käsin" and "Uudelleen" (redo the previous face); the
  long "Syötä värit käsin" stays in the review and permission views.
- **Status bar:** while the forced-dark scan is shown the status-bar icons are light, restored on
  leaving.
- **Fonts** are the variable TTFs from google/fonts; each weight picks its point on the axis. The
  OFL text ships as `res/raw/fonts_ofl.txt` and is shown on the licences screen.

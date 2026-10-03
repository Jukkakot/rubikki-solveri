## Context

`HomeScreen` is a column of five `FilledTonalButton`s built from `HomeEntry(label, onOpen)`; the
Karkki look (Fredoka/Nunito, raised shapes, `RoundIconButton` in `ui/common/Buttons.kt`) is in
place. The original wording of gallery feedback 1 was not recorded; the decisions below come from
`product.md` (scan-first app, no tall pages, playful look) and are made on the user's behalf
(autopilot).

## Goals / Non-Goals

**Goals:** a home screen with a clear hierarchy that uses the existing cube drawing and look.

**Non-Goals:** new features or routes; a "continue last solve" entry (no saved solve state
exists); settings or theme changes.

## Decisions

- **Hero = the real 3D cube** (`Cube3D` with a solved cube), not a picture. It is the app's own
  object, costs no assets and shows the Karkki colours. It spins slowly (a yaw step per frame,
  about one turn in 20 s) and pauses while dragged; spinning resumes about 2 s after release.
  Alternative: a static logo; rejected as lifeless.
- **Tap on the hero does nothing.** A hidden shortcut to the free cube would be undiscoverable;
  the free cube has its own tile.
- **Scan is the only filled (primary) button;** the four others are tonal tiles in a 2×2 grid with
  icons (edit/palette, school, timer, 3D/cube). Labels are shortened to fit a tile
  ("Syötä käsin", "Opettele", "Ajanotto", "Vapaa kuutio").
- **Solve summary** reads `timedSolves` and uses `SolveStats.best` / `format`; one line under the
  tiles, e.g. "Paras 0:42.31 · 12 ratkaisua". Guided solves are left out to keep it one line.
- **Landscape:** a `Row` with the cube on the left half and the actions on the right, chosen by
  available width/height ratio, so nothing scrolls.
- **`HomeEntry`** gains `icon` and the disabled/"coming later" path is removed with the
  requirement; `coming_soon` string is deleted if nothing else uses it.

## Risks / Trade-offs

- [Continuous spin costs battery while home is open] → the spin runs only while the screen is
  composed and resumed; a frame loop of a small canvas is cheap.
- [Spin makes screenshot tests flaky] → the screen takes a `spin` flag (off in tests).

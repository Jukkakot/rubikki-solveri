# Proposal

## Why

`ui-polish` and `scan-paint` applied the user's rule of 2026-10-06 to home, the solution screen and
the video scan: a symbol beats a word, less is more, and text stays only where it tells what is not
obvious at that moment. The other screens still carry the old amount of text and text buttons. A
review of the screenshot gallery after those two changes found these:

- **Guided scan:** a title, two lines of standing instructions ("Tuo kuutio ruudukkoon…", "Näytä
  mikä tahansa kuvaamaton puoli…"), "Kuvattu 0/6" next to the six face marks that already show it,
  and text buttons around the shutter.
- **Colour check:** a three-line paragraph at the top that stays on every face, and a "Tarkistamatta
  vielä 3 puolta" line that repeats what the face map's ticks show.
- **Manual input:** "Edellinen", "Seuraava" and "Tarkista" as three text buttons.
- **Free cube:** a standing hint ("Vedä kuutiota…"), six text buttons with letters ("Ylä U"), a
  counter-clockwise switch with a label, and three more text buttons.
- **Timer:** a long instruction in the timer area that stays after the user has timed many solves.

## What Changes

- **Guided scan, like the video scan:**
  - The camera fills the screen, with back, torch and a ⋮ menu ("Videolla", "Syötä käsin") on the
    picture and the six face marks on its top edge.
  - "Kuvattu n/6" goes.
  - One status line on the picture.
  - The standing "any face, any way" hint shows only until the first face is captured.
  - The shutter stays big and round at the bottom. Redo is a ↶ icon beside it.
- **Colour check:**
  - The paragraph becomes one line shown until the user's first action on the check ("Vertaa kuvaan
    – napauta väärää tarraa").
  - The "faces left" line goes; the face map's ticks show it.
- **Manual input:** previous and next become ‹ › icon buttons, and "Tarkista" becomes a ✓ main
  button (filled). Their names stay as descriptions for screen readers.
- **Free cube:**
  - The layer buttons show a small cube picture with the turning layer and its arrow instead of
    "Ylä U".
  - The direction switch becomes a ↻/↺ toggle icon.
  - Scramble, undo and reset become icons.
  - The drag hint shows only until the first drag.
- **Timer:** the hold-to-start instruction shows only until the first timed solve; after that the
  timer area shows the time alone.

## Capabilities

### New Capabilities
- `free-cube`: the free cube's controls (turn a layer either way, scramble, undo, reset, solve).
  Until now it had no spec of its own beyond the home entry.

### Modified Capabilities
- `camera-scan`: the one-screen layout and the scan look change. They become a full-screen camera
  with overlaid controls, the hint only until the first capture, and no "n/6" text.
- `manual-input`: face navigation uses icon buttons, and the check's instruction is shown until
  the first action.
- `progress`: the timer's instruction is shown only before the first timed solve.

## Impact

- `shared`:
  - `ui/scan/ScanScreen.kt`: layout.
  - `ui/manual/ManualInputScreen.kt`: buttons and the check's line.
  - `ui/free/FreeCubeScreen.kt`: controls.
  - `ui/progress/TimerScreen.kt`: the hint.
  - Strings in both languages.
  - New drawables for the layer buttons (drawn with the cube picture code, not image files).
- `cube`, `app` and `web`: unchanged.
- Tests: the screen tests that click these buttons by their text switch to their descriptions, and
  the screenshot tests are rendered again.

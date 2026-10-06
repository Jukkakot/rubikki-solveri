# Proposal

## Why

User feedback of 2026-10-06, with the mockups the user chose in the same talk:
- **Home:** the tagline and the best-time line add nothing.
- **Solution screen:** it is crowded. About ten things are on screen at once: title, camera switch,
  method choice, target row, holding text, cube and mirror, progress, move words and four buttons.
- **Back from a solution** goes to the colour check, which feels wrong. The aim is a scan so quick
  that colours are never fixed by hand, so going back should start a new scan.
- **Handsfree:** it waits for the demo to end before its time starts, which makes it feel slow.

General rule from the user: a symbol beats a word, less is more, but text stays where it tells
what is not obvious.

## What Changes

- **Home ("Kuutio on nappi"):**
  - The big turning cube is the scan action: a tap on it, or the round camera button on its lower
    edge, starts the scan. A drag still turns it.
  - The five other features become a row of icons with one-word labels.
  - The tagline and the best-time line are removed. The version line stays.
- **Start screen before the guide:** after a scan or other input, a start screen shows:
  - the number of moves;
  - the target (solved, a pattern, or "+" to choose);
  - the method (fastest / learn);
  - the holding position as a small picture;
  - "Aloita" and "Handsfree" with the speed.

  Choices are made once, here.
- **Guide as a media player:**
  - The guide shows the cube with its mirror, the move in words, and a timeline (move number, total,
    bar).
  - The buttons are ⏮ previous, ▶/⏸ handsfree and ⏭ done; "show again" is a ↻ icon on the cube.
  - A ⋮ menu holds camera follow, the colour check and a way back to the start screen.
  - The method row, the target row and the "Handsfree" button leave the guide.
  - The holding text shows only when the hold changes.
- **Back always means a new scan:**
  - From the guide, back goes to the start screen; from the start screen, to a new scan. This holds
    after a sure scan and after an unsure one.
  - The colour check is no longer behind the solution; it is reached from the menu.
- **Handsfree time starts with the demo:**
  - A move's time counts from when the move appears, not from the end of its demo.
  - ▶ starts handsfree at once with the remembered speed (no ready prompt); the speed is chosen on
    the start screen.

## Capabilities

### New Capabilities

### Modified Capabilities
- `app-shell`: the home screen layout changes (the cube is the scan action, an icon row, no
  tagline), and the solve summary on home is removed.
- `fast-solve`: a start screen is added, and stepping through the solution uses media-player
  controls and a menu.
- `solve-target`: the target is shown and changed on the start screen, not on the guide.
- `beginner-solver`: the method is chosen on the start screen.
- `handsfree-guide`: handsfree is started from ▶ or the start screen without a ready prompt, and
  its time starts when the move appears.
- `camera-scan`: a sure scan opens the start screen with the scan behind it, not the check.
- `video-scan`: a finished video scan does the same.

## Impact

- `shared`:
  - `ui/home/HomeScreen.kt`: layout.
  - `ui/solve/SolveScreen.kt`: start screen, player controls, menu.
  - `ui/guide/StepperState.kt` and `Handsfree.kt`: the time starts when the move appears.
  - `ui/nav/RubikkiNavHost.kt` and `Routes.kt`: the back stack after a scan, the start screen.
  - Strings in both languages.
- `app` and `web`: unchanged.
- Practice and the timer's scramble keep going straight to the guide, with no start screen.
- Tests:
  - The home, solve and navigation Compose tests change.
  - The screenshot tests for these screens are updated.

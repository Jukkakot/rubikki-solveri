# Proposal

## Why

In the phone's browser the solution screen is taller than the screen (about 560 dp of height
under the browser's bars): the user has to scroll down to press "Tein sen" after every move. The
phone app has more height but the same problem on shorter phones. The user wants every screen with
actions to fit on one screen whenever possible (2026-10-04).

## What Changes

Every screen with actions fits the screen in portrait without scrolling: solution (both methods,
camera follow, the timer's guided scramble, practice), free cube, scan, timer and lessons. Settings
and About may scroll; landscape stays as it is.

How (agreed with the user: trim the screens first, and the cube may shrink a little):
1. **Trim:** tighter spacing (12 → 8 dp between blocks), secondary lines in a smaller style, and
   per screen:
   - Solution: the step count ("Siirto 3/16") sits on the progress bar's row; the hold line
     ("Pidä kuutiota: …") stays (the spec requires it) in `bodySmall`; the method choice keeps its
     size but loses its extra vertical padding.
   - Scan, free cube, timer, lessons: spacing and secondary text only.
2. **Then shrink:** the screen's big element (3D cube, camera view, timer area, lesson picture)
   takes the height that is left, centred, never larger than today. The cube keeps one size for the
   whole solution (it is sized by the screen, not by the move).
3. **Floor:** when the big element would get under 200 dp, the screen scrolls as today instead.

Decisions (light lane, no design.md):
- Layout: a fixed column; the big element in a `weight(1f)` box, drawn as large as fits
  (`aspectRatio(…, matchHeightConstraintsFirst = true)` inside), fallback to the scrolling layout
  through `BoxWithConstraints` when the leftover height is under 200 dp. One shared helper
  (`FitColumn`) so every screen does it the same way.
- The learn method's stage card stays above the cube; the goal card is unchanged.

Modules: `shared` (the screens and the helper); tests in `app`.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `app-shell`: new requirement that screens with actions fit the screen without scrolling.

## Impact

- New `shared/.../ui/common/FitColumn.kt`; `ui/solve/SolveScreen.kt` (Stepper), `ui/guide/MoveGuide.kt`,
  `ui/guide/FollowPanel.kt`, `ui/free/FreeCubeScreen.kt`, `ui/scan/ScanScreen.kt`,
  `ui/progress/TimerScreen.kt`, lesson screens.
- Screenshots change (smaller cube on short screens).

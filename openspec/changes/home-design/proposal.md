## Why

The home screen is still the placeholder from `app-setup`: a tagline and five equal tonal buttons.
After the Karkki look refresh it is the plainest screen in the app, and nothing on it says which
action is the main one (gallery feedback 1). Every feature is built now, so the screen can be
designed around what the user actually does.

## What Changes

- A hero at the top: the app name in the heading font and the 3D cube, turning slowly on its own
  and turnable by dragging.
- **Scan the cube** becomes the one primary action: a big filled pill button with a camera icon.
- The other features (enter colours by hand, learn, timer and statistics, free cube) become a 2×2
  grid of tonal tiles, each with an icon and a short label.
- A one-line summary of the user's timed solves (best time and number of solves) when there are
  any; hidden when there are none.
- Settings stays a round icon button in the top bar; the version and install time stay small at the
  bottom.
- The "coming later" rule for unbuilt features goes away (every feature is built).
- The screen fits one phone screen in portrait; in landscape the cube sits beside the actions.

Modules: app only (no cube module changes).

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `app-shell`: the Home screen requirement is replaced by "Home screen layout" ( with hero cube, one primary action, feature
  tiles, solve summary; the "coming later" scenario removed).

## Impact

- `ui/home/HomeScreen.kt` rewritten; `HomeEntry` gains an icon and a primary flag.
- `ui/nav/RubikkiNavHost.kt`: home wiring passes the solve summary from the progress repository.
- New strings (short tile labels, summary line) in Finnish and English.
- Tests: `ShellTest` home navigation keeps working; screenshot test for home refreshed.

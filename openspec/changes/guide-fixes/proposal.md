# Proposal

## Why

Phone testing of step-settle and scan-quick-flow (2026-10-05): sometimes only the arrow shows and
no turn plays, "Näytä" can stop doing anything, and the nod is not liked (the vibration is).

## What Changes

- **Arrow with the turn:** the arrow stays on screen while the layer turns (before, it hid during
  the turn), so the movement and the arrow are seen together, and stays after the demo while the
  user turns (user's choices 1A and 2B on https://claude.ai/artifact/AzSdBmYYH1AQezkQCPXp6v).
- **Shorter pause before the demo:** back to about 0.5 s, since the arrow now shows during the turn
  (the 1.5 s pause from scan-quick-flow made it look like nothing happens).
- **"Näytä" always plays, at once:** fix the guide's animation so it keeps working after a tap lands while a
  turn is playing (likely cause: interrupting a running turn stops the animator for good; to be
  confirmed with a test first).
- **No nod:** "Tein sen" shows the next step without tilting the cube; its vibration stays.

## Capabilities

### New Capabilities

### Modified Capabilities
- `move-guide`: arrow during the turn, 0.5 s pause, show always plays, no nod.

## Impact

`shared` only: `ui/cube3d/CubeAnimator.kt`, `ui/guide/StepperState.kt`, `ui/guide/MoveGuide.kt`,
and `MoveGuideTest`. `cube`, `app` and `web` untouched.

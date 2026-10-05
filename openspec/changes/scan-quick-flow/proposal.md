# Proposal

## Why

Phone testing (2026-10-05): the scan has steps that only cost taps. Each captured face waits for
"Hyvä, seuraava", and the colour check opens after every scan although a confident scan is right
nearly always. In the guide, the arrow is seen only for a moment before the demo turns the layer,
and not after it (step-settle), so in practice the user sees no arrow.

## What Changes

- **Face review continues by itself:** after a capture the review shows as now, and "Hyvä,
  seuraava" fills up over 2.5 s and then accepts the face, the same way the colour check's "Looks
  right" fills up. A touch on the review stops it; "Kuvaa uudelleen" stays at hand. Always, since
  this page has no confidence yet (raw camera colours).
- **No colour check after a confident scan:** a valid scan with no uncertain or marked sticker goes
  straight to the solution. The check still opens, marked and without a timer, when something is
  uncertain or wrong. This reverses scan-dim-light's "check after every scan" (user, 2026-10-05).
- **Way back to the check:** after a skipped check, the check sits behind the solution, so the back
  arrow of the solution opens it with the pictures (to fix a colour if the first move does not
  match the real cube).
- **Arrow before the demo:** the pause before the automatic demo grows to 1.5 s, with the arrow on
  the cube in the state before the move; after the demo there is still no arrow (user's choice b).
  "Show" plays at once.

## Capabilities

### New Capabilities

### Modified Capabilities
- `camera-scan`: face review auto-accept; the check only for an unsure scan; back from the solution
  to the check.
- `move-guide`: a longer pause with the arrow before the automatic demo.

## Impact

`shared` only: `ui/scan/ScanScreen.kt` (face review timer), `ui/nav/RubikkiNavHost.kt` (scan
result routing, back stack), `ui/manual/ManualInputScreen.kt` (the check no longer needs its
timer), `ui/guide/StepperState.kt` (demo delay), their tests. `cube`, `app` and `web` untouched.

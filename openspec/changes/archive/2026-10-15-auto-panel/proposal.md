# Proposal

## Why

The guide's big central ▶ starts the automatic mode, and a new user presses it expecting one move forward
(user 2026-10-15). From the layout mockups (artifact A58HMxYQpGnEdgBwA7hPUb) the user picked D with the speed
chosen in the same place: the big button always goes one move, the automatic mode sits behind a small
"Auto" button that opens a panel with the speed.

## What Changes

- Controls: small ⏮ and a wide primary "Seuraava ⏭" (reads "Pysäytä" while the automatic mode runs). The
  big ▶ goes.
- A small outlined "Auto" button beside the move words (where the mode is offered) opens a bottom panel:
  title, one line on what it does and how to stop it, a speed slider of five steps in seconds per move, and
  "Käynnistä".
- While it runs: a band above the move words "Automaattinen · 2,1 s / siirto · kosketa pysäyttääksesi".
- The speed leaves the ⋮ menu. Five speeds instead of three (the old ones keep their names, so a saved
  choice stays).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `fast-solve`: guide controls (wide "Seuraava", ↻ beside the words), menu without speed.
- `handsfree-guide`: started from the "Auto" panel with a five-step speed slider; the running band.

## Impact

Module `shared` (solve screen, speeds, strings); `app` screen tests. No storage change (speeds stored by
name). No change to `cube`.

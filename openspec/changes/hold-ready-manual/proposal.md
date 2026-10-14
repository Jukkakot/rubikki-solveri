# Proposal

## Why

The user (2026-10-14) wants the start of a solve to be visual: a clear "hold the cube like this" picture
with hands drawn on the cube, and the solve begins only when they say they are ready. They also want the
guide to always go one move at a time by default; the automatic mode (handsfree) is something switched on
separately, and any touch switches it off. Today the start screen offers "Handsfree" as a big button beside
"Aloita" and shows the hold only as a small picture with a line of text.

## What Changes

- A new hold step between the start screen and the guide: a big cube in the holding position with two drawn
  hands holding it from the sides (thumbs on the front), the hold in words, and "Olen valmis". Only that
  button opens the guide (move 1, manual). Back returns to the start screen.
- The start screen loses the small hold row and the "Handsfree" button with its speed row; "Aloita" opens
  the hold step.
- The guide always starts in the manual mode. The automatic mode is shown to the user as "Automaattinen"
  (English "Autoplay") instead of "Handsfree"; it starts only from ▶ in the guide. Its speed moves to the
  guide's ⋮ menu (still remembered between solves). A touch anywhere stops it, as today.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `fast-solve`: the start screen no longer shows the hold or handsfree; a new hold step with hands and
  "Olen valmis" before the guide; the guide menu offers the automatic speed.
- `handsfree-guide`: started only from ▶ in the guide (no start-screen button), the speed chosen in the
  guide menu, named "Automaattinen".

## Impact

Module `shared` (solve screen, a new hold picture composable, strings) and `app` tests. No change to `cube`.
The browser and the phone app both get it through the shared screens.

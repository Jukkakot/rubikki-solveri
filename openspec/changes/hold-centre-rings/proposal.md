# Proposal

## Why

The user found the hold step's drawn hands ugly (2026-10-14) and wants it to stress that "the green side" means
the side whose centre piece is green. From the mockup page (artifact HZaSoKXGpgM7EttZqpAsA4) the user took the
recommended combination K6: own hands seen from the user's eyes (J5), rings around the centre pieces (K1), the
cube turning into the hold first (E1).

## What Changes

- Hands redrawn: forearms from the bottom of the picture like the user's own arms, palms at the cube's sides
  (behind it), thumbs on the front face.
- Pulsing rings around the front and top centre pieces, and a line under the hold words: the centre piece tells
  the side's colour.
- On opening, the cube turns from a tilted pose into the holding view (about 1.2 s); the hands and rings fade in
  after it. Animations off: shown at once, no pulse.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `fast-solve`: the hold step's picture (hands, rings, turn-in) and the centre-piece line.

## Impact

Module `shared` only (hold picture composable, one string pair, hold pane); `app` screenshot and screen tests.
No change to `cube`.

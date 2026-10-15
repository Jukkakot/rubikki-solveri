# Proposal

## Why

The hold picture shows three sides (front, right, top), but only the front and top centres were ringed and
named. The user (2026-10-15) wants the right side shown too, as a check that the cube is held right. The
user first thought of the green/red edge as facing them ("green left, red right"); that was turned down because
the move words call the green side "etupuoli" and the hidden orange side "vasen puoli", so the hold says
"edessä" and a hint explains why the picture is turned a little.

## What Changes

- A third pulsing ring around the right side's centre piece.
- The hold in words: "<front> edessä, <right> oikealla, <top> ylhäällä." (the user's own centre colours).
- A hint line: "Käännä kuutiota vähän, niin näet oikean sivun." The centre-piece line stays.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `fast-solve`: the hold step rings and names three centres and gives the turn hint.

## Impact

Module `shared` (hold picture, hold pane, strings); `app` start screen test. No change to `cube`.

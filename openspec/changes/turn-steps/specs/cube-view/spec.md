## MODIFIED Requirements

### Requirement: Move animation
Every move SHALL be shown as a smooth turn of the moving layers (or the whole cube for a
rotation) about the right axis in the right direction. A quarter turn SHALL take about 0.3 s. A
half turn SHALL play as two quarter steps in the same direction, each like a quarter turn, with a
pause of about 0.25 s between them, so it is plain that the layer turns twice. Moves requested
during an animation SHALL queue and play in order. When the phone's animations are switched off,
moves SHALL apply at once.

#### Scenario: Animated turn
- **WHEN** the move R is played
- **THEN** only the right layer turns, away from the viewer at the top, and the cube ends in the state after R

#### Scenario: Half turn in two steps
- **WHEN** the move R2 is played
- **THEN** the right layer turns a quarter, stops briefly, turns another quarter the same way, and the cube ends in the state after R2

#### Scenario: Queued moves
- **WHEN** three moves are requested quickly
- **THEN** they play one after another and the cube ends in the state after all three

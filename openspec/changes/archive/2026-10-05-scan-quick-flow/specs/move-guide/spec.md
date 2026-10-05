# Spec Delta

## MODIFIED Requirements

### Requirement: Demo and replay
When a new move is presented, the cube SHALL first show the state before the move with the arrow
for about a second and a half; then its turn SHALL play once by itself, and the cube SHALL stay in
the state after the move, as the user's cube will be once they have turned it. "Show" SHALL start
again from the state before the move and play it at once, ending after the move.

#### Scenario: Automatic demo
- **WHEN** the next move appears
- **THEN** the arrow is shown on the cube before the move for about a second and a half, then the move plays once and the cube stays as it is after the move

#### Scenario: Show again
- **WHEN** the user taps show after the demo has ended
- **THEN** the cube jumps back to before the move, plays it and stays after the move

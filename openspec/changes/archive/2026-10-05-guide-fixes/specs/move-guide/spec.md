# Spec Delta

## RENAMED Requirements

- FROM: `### Requirement: Nod on a step change`
- TO: `### Requirement: Step change without a replay`

## MODIFIED Requirements

### Requirement: Direction arrow
A curved arrow SHALL be drawn in the direction of the turn, covering a quarter of a circle for a
quarter turn and half a circle for a half turn, with its arrowhead at the end. When the turning
face can be seen, the arrow SHALL be on that face. When it faces away from the view (such as the
back), the arrow SHALL go around the outside of the turning layer, where it can be seen, instead
of being drawn over the other side of the cube. The arrow SHALL be shown the whole time a move is
presented: before the demo, while the turn animates (so the turn and the arrow are seen together)
and after the demo, until the user moves on.

#### Scenario: Clockwise arrow
- **WHEN** a clockwise turn of the front is presented in the default view
- **THEN** the arrow, seen on screen, goes clockwise

#### Scenario: Half turn arrow
- **WHEN** a half turn is presented
- **THEN** the arrow covers half a circle

#### Scenario: Back turn arrow
- **WHEN** a turn of the back is presented in the holding view
- **THEN** the arrow runs around the outside of the back layer, visible beside the cube, and none is drawn over the front

#### Scenario: Arrow during the turn
- **WHEN** the demo turns the layer
- **THEN** the arrow stays on screen while the layer turns

#### Scenario: No arrow after the demo
- **WHEN** the demo of a move has ended and the cube stays after the move
- **THEN** the arrow is not taken away: it stays while the user turns their cube (reversed 2026-10-05)

### Requirement: Demo and replay
When a new move is presented, the cube SHALL first show the state before the move with the arrow
for about half a second; then its turn SHALL play once by itself, and the cube SHALL stay in the
state after the move, as the user's cube will be once they have turned it. "Show" SHALL always
start again from the state before the move and play it at once, ending after the move, also
while a demo is still turning and after earlier taps of done, show or back.

#### Scenario: Automatic demo
- **WHEN** the next move appears
- **THEN** the arrow is shown on the cube before the move for about half a second, then the move plays once with the arrow and the cube stays as it is after the move

#### Scenario: Show again
- **WHEN** the user taps show after the demo has ended
- **THEN** the cube jumps back to before the move, plays it and stays after the move

#### Scenario: Show after quick taps
- **WHEN** the user has tapped done while a demo was turning and then taps show on the next move
- **THEN** the next move plays

### Requirement: Step change without a replay
When the guide moves to the next step (done, or a move detected by camera follow), the cube
SHALL NOT replay a turn; it SHALL show the state of the new step at once, without a nod or other
motion of the whole cube. "Done" keeps its short confirming vibration.

#### Scenario: Done after the demo
- **WHEN** the demo of a move has ended and the user taps done
- **THEN** the cube does not turn again or tilt, the phone vibrates, and the next move appears and demos

#### Scenario: Done during the demo
- **WHEN** the user taps done while the demo is still turning
- **THEN** the cube jumps to the state after the move and the next move appears

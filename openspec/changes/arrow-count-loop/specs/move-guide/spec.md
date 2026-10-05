# Spec Delta

## MODIFIED Requirements

### Requirement: Direction arrow
A curved arrow SHALL be drawn in the direction of the turn, covering a quarter of a circle for a
quarter turn and half a circle for a half turn, with its arrowhead at the end. In the middle of the
arc the arrow SHALL carry a badge with the number of quarter turns, "×1" or "×2", for every move.
When the turning face can be seen, the arrow SHALL be on that face. When it faces away from the view
(such as the back), the arrow SHALL go around the outside of the turning layer, where it can be seen,
instead of being drawn over the other side of the cube. The arrow SHALL be shown the whole time a move
is presented: before the demo, while the turn animates (so the turn and the arrow are seen together)
and after the demo, until the user moves on.

#### Scenario: Clockwise arrow
- **WHEN** a clockwise turn of the front is presented in the default view
- **THEN** the arrow, seen on screen, goes clockwise

#### Scenario: Half turn arrow
- **WHEN** a half turn is presented
- **THEN** the arrow covers half a circle and its badge says "×2"

#### Scenario: Quarter turn count
- **WHEN** a quarter turn is presented
- **THEN** the badge in the middle of the arrow says "×1"

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
for about half a second; then its turn SHALL play by itself, and the cube SHALL stay in the state
after the move, as the user's cube will be once they have turned it. Three seconds after a demo (or
a "Show") has ended, the cube SHALL jump back to before the move and play it again, and keep doing
so until the user moves on; dragging the cube does not stop it. "Show" SHALL always start again from
the state before the move and play it at once, ending after the move, also while a demo is still
turning and after earlier taps of done, show or back. With animations off nothing SHALL repeat.

#### Scenario: Automatic demo
- **WHEN** the next move appears
- **THEN** the arrow is shown on the cube before the move for about half a second, then the move plays with the arrow and the cube stays as it is after the move

#### Scenario: Demo repeats
- **WHEN** the demo of a move has ended and the user does nothing
- **THEN** after three seconds the move plays again from before it, and again three seconds after that

#### Scenario: Show again
- **WHEN** the user taps show after the demo has ended
- **THEN** the cube jumps back to before the move, plays it and stays after the move, and the next repeat comes three seconds after it

#### Scenario: Show after quick taps
- **WHEN** the user has tapped done while a demo was turning and then taps show on the next move
- **THEN** the next move plays

# Spec Delta

## MODIFIED Requirements

### Requirement: Demo and replay
When a new move is presented, its turn SHALL play once by itself after a short pause, and the
cube SHALL stay in the state after the move, as the user's cube will be once they have turned it.
"Show" SHALL start again from the state before the move and play it, ending after the move.

#### Scenario: Automatic demo
- **WHEN** the next move appears
- **THEN** it plays once and the cube stays as it is after the move

#### Scenario: Show again
- **WHEN** the user taps show after the demo has ended
- **THEN** the cube jumps back to before the move, plays it and stays after the move

### Requirement: Mirror
In the guide of both methods (not in camera follow), the 3D scene SHALL show a framed mirror
behind the guide cube, above it and a little to the left, turned so that in the holding view the cube's
reflection is seen in the middle of the glass. The reflection SHALL be a true reflection of the
cube in the mirror's plane (showing sides the main view hides, such as the back), with the same
colours, highlight and turning animation as the cube, but without the direction arrow. The mirror
SHALL stay in place on the screen like a mirror on a wall: dragging turns only the cube, and the
reflection follows the cube. The mirror SHALL NOT cover the cube and SHALL have no label.

#### Scenario: Back move in the mirror
- **WHEN** a turn of the back is presented
- **THEN** the mirror shows the back layer highlighted and turning, without an arrow, while the main cube stays in the holding view

#### Scenario: Mirror stays still
- **WHEN** the user drags the main cube
- **THEN** the mirror stays where it is and its reflection shows the cube in its new orientation

## ADDED Requirements

### Requirement: Nod on a step change
When the guide moves to another step (done, back, or a move detected by camera follow), the cube
SHALL NOT replay a turn; it SHALL show the state of the new step at once with a small nod, a tilt
of a few degrees and back in about a third of a second. With animations off there SHALL be no nod.

#### Scenario: Done after the demo
- **WHEN** the demo of a move has ended and the user taps done
- **THEN** the cube does not turn again, it nods, and the next move appears and demos

#### Scenario: Done during the demo
- **WHEN** the user taps done while the demo is still turning
- **THEN** the cube jumps to the state after the move, nods, and the next move appears

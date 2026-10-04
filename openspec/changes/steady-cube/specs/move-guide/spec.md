## MODIFIED Requirements

### Requirement: View follows the move
In the learn method, the view SHALL keep the holding position (white on top, green in front) and
turn so that the turning side is visible: top, front and right from the front-right, left from the
front-left, back from behind, bottom from below. Changes of view SHALL animate. In the fast method
the steady view applies instead.

#### Scenario: Back move
- **WHEN** a turn of the back is presented in the learn method
- **THEN** the view swings round so that the back face is visible

## ADDED Requirements

### Requirement: Steady view
In the fast method (shortest solution and the timer's guided scramble), including its camera
follow, the guide cube SHALL always be shown in the holding position (white on top, green in front,
seen from the front a little from the right and above), for every move. The view SHALL never turn
by itself; only the user's drag turns it. The turning layer SHALL stay highlighted.

#### Scenario: Back move stays in view
- **WHEN** a turn of the back is presented in the fast method
- **THEN** the cube stays in the same view as for the previous move, with the back layer highlighted

#### Scenario: Sequence of moves
- **WHEN** the user steps through R, B, L and D in the fast method
- **THEN** the cube's view is the same for all four moves

### Requirement: Mirror
In the fast method's guide (not in camera follow), a small mirror cube SHALL always be shown next
to the guide cube. It SHALL show the same cube from behind and the left, mirrored left to right as
a mirror would, with the same highlight, arrow and animation as the main cube, and SHALL NOT
respond to drags.

#### Scenario: Back move in the mirror
- **WHEN** a turn of the back is presented
- **THEN** the mirror shows the back layer highlighted while the main cube stays in the holding view

#### Scenario: Mirror stays still
- **WHEN** the user drags the main cube
- **THEN** the mirror's view does not change

### Requirement: Reset orientation
In the fast method, after the user has dragged the guide cube to another view, a button SHALL
appear on the cube that turns it back to the holding view with an animation. The button SHALL be
hidden while the cube is in the holding view. The view SHALL NOT return by itself.

#### Scenario: Drag and reset
- **WHEN** the user drags the cube round and taps the reset button
- **THEN** the cube animates back to the holding view and the button disappears

#### Scenario: Next move keeps the user's view
- **WHEN** the user has dragged the cube and steps to the next move
- **THEN** the view stays where the user left it and the reset button stays visible

## REMOVED Requirements

### Requirement: View follows the move
**Reason**: The view is the same everywhere (user, 2026-10-04): the learn method also keeps the
steady holding view, and the mirror shows the hidden sides.
**Migration**: The Steady view, Mirror and Reset orientation requirements now cover both methods.

## MODIFIED Requirements

### Requirement: Steady view
In both methods (shortest solution, learn step by step, the timer's guided scramble and practice),
including camera follow, the guide cube SHALL always be shown in the holding position (the current
hold's front centre towards the user and its top centre up, seen from the front a little from the
right and above), for every move. The view SHALL never turn by itself; only the user's drag turns
it. A whole-cube turn SHALL be shown as the cube turning in that view. The turning layer SHALL stay
highlighted.

#### Scenario: Back move stays in view
- **WHEN** a turn of the back is presented in either method
- **THEN** the cube stays in the same view as for the previous move, with the back layer highlighted

#### Scenario: Sequence of moves
- **WHEN** the user steps through R, B, L and D in the learn method
- **THEN** the cube's view is the same for all four moves

### Requirement: Mirror
In the guide of both methods (not in camera follow), the 3D scene SHALL show a framed mirror
behind the guide cube, up and to the left of it, turned so that in the holding view the cube's
reflection is seen in the middle of the glass. The reflection SHALL be a true reflection of the
cube in the mirror's plane (showing sides the main view hides, such as the back), with the same
colours, highlight, arrow and turning animation as the cube. The mirror SHALL stay in place on
the screen like a mirror on a wall: dragging turns only the cube, and the reflection follows the
cube. The mirror SHALL NOT cover the cube and SHALL have no label.

#### Scenario: Back move in the mirror
- **WHEN** a turn of the back is presented
- **THEN** the mirror shows the back layer highlighted and turning while the main cube stays in the holding view

#### Scenario: Mirror stays still
- **WHEN** the user drags the main cube
- **THEN** the mirror stays where it is and its reflection shows the cube in its new orientation

### Requirement: Reset orientation
In both methods, after the user has dragged the guide cube to another view, a button SHALL appear
on the cube that turns it back to the holding view with an animation. The button SHALL be hidden
while the cube is in the holding view. The view SHALL NOT return by itself.

#### Scenario: Drag and reset
- **WHEN** the user drags the cube round and taps the reset button
- **THEN** the cube animates back to the holding view and the button disappears

#### Scenario: Next move keeps the user's view
- **WHEN** the user has dragged the cube and steps to the next move
- **THEN** the view stays where the user left it and the reset button stays visible

## MODIFIED Requirements

### Requirement: Mirror
In the fast method's guide (not in camera follow), the 3D scene SHALL show a framed mirror behind
the guide cube, up and to the left of it, turned so that in the holding view the cube's
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

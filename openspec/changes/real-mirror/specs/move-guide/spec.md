## MODIFIED Requirements

### Requirement: Mirror
In the fast method's guide (not in camera follow), the 3D scene SHALL show a framed mirror behind
the guide cube, up and to the left of it, turned so that in the holding view the cube's
reflection is seen in the middle of the glass. The reflection SHALL be a true reflection of the
cube in the mirror's plane (showing sides the main view hides, such as the back), with the same
colours, highlight, arrow and turning animation as the cube. The mirror SHALL be part of the
scene: dragging the cube turns the mirror with it and the reflection stays true; when the glass
faces away from the viewer, the mirror SHALL NOT be drawn. The mirror SHALL NOT cover the cube.

#### Scenario: Back move in the mirror
- **WHEN** a turn of the back is presented
- **THEN** the mirror shows the back layer highlighted and turning while the main cube stays in the holding view

#### Scenario: Mirror stays still
- **WHEN** the user drags the main cube a little
- **THEN** the mirror stays still relative to the cube (it turns with the scene) and still shows the cube's true reflection

#### Scenario: Mirror seen from behind
- **WHEN** the user drags the cube round so the mirror's glass faces away
- **THEN** the mirror is not drawn, and it appears again after the reset button is pressed

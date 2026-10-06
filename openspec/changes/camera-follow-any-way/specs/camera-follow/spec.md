## MODIFIED Requirements

### Requirement: Camera mode
The solution screen SHALL offer a camera mode showing the live camera picture without a grid, the
current move in words, a small 3D guide cube showing the move, and the usual previous, show and
done buttons. The user MAY hold the cube any way round, anywhere in the picture; the screen SHALL
NOT ask for a particular side towards the camera or on top.

#### Scenario: Switch to camera mode
- **WHEN** the user turns on camera mode during a solution
- **THEN** the camera picture appears without a grid, with the current move and the small guide cube

#### Scenario: Cube held another way
- **WHEN** the user holds the cube with blue towards the camera and yellow on top
- **THEN** following goes on as usual, without asking to turn the cube to green and white

### Requirement: Arrow on the real cube
The current move SHALL be drawn on the real cube in the picture, on the side most towards the
camera that shows the turn: on the turning side itself as a round arrow in the turning direction,
on a neighbouring side as an arrow along the turning layer's row of stickers in the direction they
move. A half turn SHALL be marked "2×". The arrow SHALL stay on the cube as the cube moves in the
picture. When no side in view shows the turn, no arrow SHALL be drawn.

#### Scenario: Right turn
- **WHEN** the cube is held the holding-view way, front straight to the camera, and the move is "turn the right side clockwise"
- **THEN** an upward arrow is drawn along the right column of the front side

#### Scenario: Top turn
- **WHEN** the cube is held the holding-view way, front straight to the camera, and the move is "turn the top clockwise"
- **THEN** a leftward arrow is drawn along the top row of the front side

#### Scenario: Turning side towards the camera
- **WHEN** the move turns the side that faces the camera
- **THEN** a round arrow in the turning direction is drawn on that side

#### Scenario: Turning side seen from the edge
- **WHEN** the move turns the side on the right of the side facing the camera
- **THEN** an arrow is drawn along the right-hand column of the facing side, in the direction its stickers move

#### Scenario: Cube moves in the picture
- **WHEN** the user moves or tilts the cube while the move is shown
- **THEN** the arrow follows the turning layer on the real cube

### Requirement: Detecting the move
When the sides in view match the cube after the current move for a short stable moment, the app
SHALL advance to the next move with a vibration, however the cube is held. When they match the
result of a different turn, it SHALL say what was done and which turn undoes it.

#### Scenario: Move done
- **WHEN** the user makes the shown move with the cube held any way in view
- **THEN** the app advances to the next move by itself

#### Scenario: Wrong direction
- **WHEN** the user turns the turning side the opposite way
- **THEN** the app says the turn went the wrong way and shows how to undo it

### Requirement: Self-calibration
While following, the colour reading SHALL adapt to the light using the stickers whose colours are
known from the cube, on every side in view.

#### Scenario: Warm light
- **WHEN** the light makes white read as yellowish
- **THEN** after a few frames of known sides, white is read as white

## REMOVED Requirements

### Requirement: Moves not visible from the front
**Reason**: With the cube held any way there is no fixed front; replaced by "Moves not in view".
**Migration**: See "Moves not in view".

### Requirement: Cube in the grid
**Reason**: The grid is gone; replaced by "Cube in view".
**Migration**: See "Cube in view".

## ADDED Requirements

### Requirement: Moves not in view
When no side in view changes with the current move, the screen SHALL ask the user to turn the cube
so that the turning side shows, or to tap done after turning.

#### Scenario: Only the opposite side in view
- **WHEN** the move turns the side away from the camera and only the side facing the camera is in view
- **THEN** the screen asks to turn the cube so the turning side shows, or to tap done after turning

### Requirement: Cube in view
A frame in which no cube side is found SHALL be ignored (no advance, no wrong-move notice, no
learning of colours), and the screen SHALL ask the user to show the cube to the camera. When the
sides in view cannot be placed on the cube (for example one evenly coloured side alone and the cube
was not seen a moment before), the screen SHALL ask the user to tilt the cube so that two sides
show.

#### Scenario: Cube out of view
- **WHEN** the user lowers the cube out of the picture
- **THEN** the app asks to show the cube and does not advance or report a wrong move

#### Scenario: Back in view
- **WHEN** a side of the cube is in the picture again
- **THEN** following continues as before

#### Scenario: One plain side only
- **WHEN** the only side in view is all one colour and the cube's turn is not known from a moment before
- **THEN** the app asks to tilt the cube so two sides show, and draws no arrow until it can

### Requirement: Words and guide cube follow the hold
While the cube in the picture is held the holding-view way, the move SHALL be worded as elsewhere in
the app. While it is held another way, the move text SHALL name the turning side by its centre
colour and refer to the arrow for the direction. The small 3D guide cube SHALL turn to show the
cube the way the real cube is held, and return to the holding view while no cube is in view.

#### Scenario: Held another way
- **WHEN** the cube is held with blue towards the camera and the move turns the red side
- **THEN** the text names the red side and tells to turn it the way of the arrow, and the guide cube shows blue towards the user

#### Scenario: Held the usual way
- **WHEN** the cube is held the holding-view way
- **THEN** the move is worded as in the guide on screen

### Requirement: Camera light
Camera follow SHALL set the camera as the video scan does: light measured and focus set on the cube,
then locked, and a torch button in the picture.

#### Scenario: Dim room
- **WHEN** the user turns on the torch during camera follow
- **THEN** the torch lights and the camera measures the light again

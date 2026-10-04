# move-guide Specification

## Purpose
Presents one move at a time so clearly that a beginner turns the right layer the right way on the
first try: highlighted layer, direction arrow, a view that shows the turning side, words, motion
and touch feedback.

## Requirements

### Requirement: Highlighted layer
While a move is presented, the stickers of the layer that turns SHALL be shown at full colour and
the rest of the cube dimmed.

#### Scenario: Right layer highlighted
- **WHEN** the move "turn the right side" is presented
- **THEN** the nine right-layer cubies are at full colour and the others are dimmed

### Requirement: Direction arrow
A curved arrow SHALL be drawn on the turning face, in the direction of the turn, covering a
quarter of a circle for a quarter turn and half a circle for a half turn, with its arrowhead at
the end. The arrow SHALL be hidden while the turn animates.

#### Scenario: Clockwise arrow
- **WHEN** a clockwise turn of the front is presented in the default view
- **THEN** the arrow, seen on screen, goes clockwise

#### Scenario: Half turn arrow
- **WHEN** a half turn is presented
- **THEN** the arrow covers half a circle

### Requirement: View follows the move
In the learn method, the view SHALL keep the holding position (white on top, green in front) and
turn so that the turning side is visible: top, front and right from the front-right, left from the
front-left, back from behind, bottom from below. Changes of view SHALL animate. In the fast method
the steady view applies instead.

#### Scenario: Back move
- **WHEN** a turn of the back is presented in the learn method
- **THEN** the view swings round so that the back face is visible

### Requirement: Demo and replay
When a new move is presented, its turn SHALL play once by itself after a short pause, and the
cube SHALL return to the state before the move. "Show" SHALL replay it.

#### Scenario: Automatic demo
- **WHEN** the next move appears
- **THEN** it plays once and the cube returns to before the move

### Requirement: Haptics
Confirming a move SHALL give a short confirming vibration, and the end of a demo a light tick,
using the phone's standard haptic patterns. A demo of a half turn SHALL tick after each of its
two quarter steps.

#### Scenario: Done vibrates
- **WHEN** the user taps done
- **THEN** the phone gives a confirming vibration

#### Scenario: Half turn demo ticks twice
- **WHEN** a half turn is demoed
- **THEN** the phone gives a light tick after the first quarter step and another at the end

### Requirement: Notation option
Settings SHALL have a switch to show the standard move notation, off by default. When on, the
notation SHALL appear small under the move's words.

#### Scenario: Notation on
- **WHEN** the switch is on and the move is a counter-clockwise turn of the right side
- **THEN** "R'" is shown under the words

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

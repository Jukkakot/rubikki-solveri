# Spec Delta

## Purpose

Presents one move at a time so clearly that a beginner turns the right layer the right way on the
first try: highlighted layer, direction arrow, a view that shows the turning side, words, motion
and touch feedback.

## ADDED Requirements

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
The view SHALL keep the holding position (white on top, green in front) and turn so that the
turning side is visible: top, front and right from the front-right, left from the front-left,
back from behind, bottom from below. Changes of view SHALL animate.

#### Scenario: Back move
- **WHEN** a turn of the back is presented
- **THEN** the view swings round so that the back face is visible

### Requirement: Demo and replay
When a new move is presented, its turn SHALL play once by itself after a short pause, and the
cube SHALL return to the state before the move. "Show" SHALL replay it.

#### Scenario: Automatic demo
- **WHEN** the next move appears
- **THEN** it plays once and the cube returns to before the move

### Requirement: Haptics
Confirming a move SHALL give a short confirming vibration, and the end of a demo a light tick,
using the phone's standard haptic patterns.

#### Scenario: Done vibrates
- **WHEN** the user taps done
- **THEN** the phone gives a confirming vibration

### Requirement: Notation option
Settings SHALL have a switch to show the standard move notation, off by default. When on, the
notation SHALL appear small under the move's words.

#### Scenario: Notation on
- **WHEN** the switch is on and the move is a counter-clockwise turn of the right side
- **THEN** "R'" is shown under the words

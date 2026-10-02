# Spec Delta

## Purpose

The single model of a 3×3 cube used by every feature: its stickers, how moves change them, how
moves are written, and whether a colouring is a cube that can be solved.

## ADDED Requirements

### Requirement: Cube state
A cube state SHALL consist of the colours of all 54 stickers, nine per face, using six colours.
The solved cube in the app's holding position SHALL have white on top, green in front, red on the
right, orange on the left, blue at the back and yellow at the bottom.

#### Scenario: Solved cube
- **WHEN** a solved cube is created
- **THEN** every face has nine stickers of one colour, white on top and green in front

### Requirement: Face and slice moves
The model SHALL apply the face turns U, D, R, L, F, B, the slice turns M, E, S and the wide turns
(Uw/u, Dw/d, Rw/r, Lw/l, Fw/f, Bw/b), each clockwise as seen looking at the named face, counter-
clockwise with ', and a half turn with 2. M follows L, E follows D and S follows F.

#### Scenario: Four quarter turns
- **WHEN** any single move is applied four times to any cube
- **THEN** the cube is unchanged

#### Scenario: Known cycle
- **WHEN** the sequence R U R' U' is applied six times to a solved cube
- **THEN** the cube is solved again

#### Scenario: Turn direction
- **WHEN** U is applied to a solved cube
- **THEN** the top row of the front face shows the colour of the right face

### Requirement: Cube rotations
The model SHALL apply the whole-cube rotations x, y and z (following R, U and F), which change
which colour is on top or in front without changing whether the cube is solved.

#### Scenario: Rotation keeps solved
- **WHEN** x is applied to a solved cube
- **THEN** the cube is still solved and green is on top

### Requirement: Notation
The model SHALL read and write move sequences in standard notation, separated by spaces, and SHALL
reject unknown tokens with the position of the first bad token. Writing a parsed sequence and
reading it back SHALL give the same moves.

#### Scenario: Parse and print
- **WHEN** the text "R U2 R' u x' M2" is read
- **THEN** six moves result and writing them gives the same text

#### Scenario: Bad token
- **WHEN** the text "R Q U" is read
- **THEN** it is rejected, naming the token "Q" at position 2

### Requirement: Inverse and simplification
The model SHALL give the inverse of a move sequence, which undoes it, and SHALL simplify a
sequence by merging consecutive turns of the same layer and dropping turns that cancel out.

#### Scenario: Inverse undoes
- **WHEN** a random sequence and then its inverse are applied to a solved cube
- **THEN** the cube is solved

#### Scenario: Merge and cancel
- **WHEN** "R R U U' F2 F2 L" is simplified
- **THEN** the result is "R2 L"

### Requirement: Validity check
Before solving, the model SHALL tell whether a colouring is a solvable cube and, if not, give the
first reason found, in this order: a colour not used exactly nine times; centres that are not
six different colours in a real arrangement; a corner or edge whose colours cannot exist; a piece
that appears twice; a twisted corner; a flipped edge; two pieces swapped. Where a reason concerns
particular stickers, the result SHALL name them.

#### Scenario: Solved and scrambled cubes are valid
- **WHEN** a solved cube or any scrambled cube is checked
- **THEN** it is valid

#### Scenario: Wrong colour count
- **WHEN** one white sticker is painted yellow
- **THEN** the result says white is used 8 times and yellow 10 times

#### Scenario: Impossible piece
- **WHEN** a corner's stickers are white, yellow and red
- **THEN** the result is an impossible piece naming that corner's stickers

#### Scenario: Twisted corner
- **WHEN** one corner of a solved cube is twisted in place
- **THEN** the result is a twisted corner

#### Scenario: Flipped edge
- **WHEN** one edge of a solved cube is flipped in place
- **THEN** the result is a flipped edge

#### Scenario: Swapped pieces
- **WHEN** two edges of a solved cube are swapped
- **THEN** the result is swapped pieces

### Requirement: Scramble
The model SHALL generate a random scramble of a requested length in which no two consecutive moves
turn the same face, and no three consecutive moves turn the same axis.

#### Scenario: Scramble shape
- **WHEN** a scramble of 25 moves is generated
- **THEN** it has 25 face turns and no two neighbours turn the same face

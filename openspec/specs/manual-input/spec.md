# manual-input Specification

## Purpose
Lets the user enter their real cube's colours by hand, face by face, and tells them precisely what
is wrong before they try to solve it.

## Requirements

### Requirement: Face-by-face painting
The manual input screen SHALL show one face at a time as a large 3×3 grid, a palette of the six
colours and a hint how to hold the cube so that the face looks at the user (for example: "green
centre towards you, white on top"). Tapping a cell SHALL paint it with the selected colour. The
centre cells SHALL be fixed to the holding position's colours.

#### Scenario: Paint a sticker
- **WHEN** the user selects red and taps the top-left cell of the front face
- **THEN** that sticker becomes red

#### Scenario: Centres are fixed
- **WHEN** the user taps a centre cell
- **THEN** its colour does not change

### Requirement: Face navigation and preview
A map of all six faces SHALL show progress and let the user jump to any face; next/previous
buttons SHALL go through the faces in the order front, right, back, left, top, bottom. A 3D
preview SHALL turn to show the face being edited.

#### Scenario: Next face
- **WHEN** the user is on the front face and taps next
- **THEN** the right face is shown with the hint "red centre towards you, white on top"

### Requirement: Colour counts
The screen SHALL show for each colour how many stickers have it, out of nine, and mark counts
over nine.

#### Scenario: Too many of a colour
- **WHEN** ten stickers are painted white
- **THEN** white shows 10/9 marked as an error

### Requirement: Check
When every sticker has a colour, the user SHALL be able to check the cube. An invalid cube SHALL
give a short message naming the problem in plain words and mark the stickers concerned where
known; a valid cube SHALL be accepted and offered for solving.

#### Scenario: Unfinished cube
- **WHEN** some stickers have no colour
- **THEN** the check is not available

#### Scenario: Invalid cube
- **WHEN** a cube with a twisted corner is checked
- **THEN** a message says one corner is twisted

#### Scenario: Valid cube
- **WHEN** a valid cube is checked
- **THEN** it is accepted

### Requirement: Editing helpers
The screen SHALL offer to clear all stickers and to fill in a solved cube, and SHALL keep the
painting when the screen is rotated.

#### Scenario: Clear
- **WHEN** the user clears the cube
- **THEN** every sticker except the centres has no colour

## MODIFIED Requirements

### Requirement: Moves in words
Each move SHALL be described as the user sees it from the front in the holding position, in the
app's language: the top and bottom layers turn to the left or right, the right and left sides
turn up or down, the back side by which way its top row moves, and only the front by clockwise or
counter-clockwise. Half turns SHALL say half a turn. The holding position (white on top, green in
front) SHALL be stated on the screen.

#### Scenario: Describe a move
- **WHEN** the current move is a counter-clockwise turn of the top (U')
- **THEN** it reads "Turn the top layer to the right"

#### Scenario: Back move
- **WHEN** the current move is a clockwise turn of the back (B)
- **THEN** it reads "Turn the back side so its top row moves to the left"

#### Scenario: Front move
- **WHEN** the current move is a clockwise turn of the front (F)
- **THEN** it reads "Turn the front side clockwise"

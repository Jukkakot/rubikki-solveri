## MODIFIED Requirements

### Requirement: Moves in words
Each move SHALL be described in the app's language, the same way everywhere in the app (both
solution methods, camera follow, lessons): as the user sees it in the holding view, the top and
bottom layers turn to the left or right, the right and left sides turn up or down, the back side
by which way its top row moves, and the front clockwise or counter-clockwise; half turns say half
a turn; a whole-cube turn names the centre that comes towards the user and the one on top. Other
texts that name a layer or a direction (step notes, lesson texts, tips) SHALL use the same terms.
The holding position SHALL be stated on the screen.

#### Scenario: Top move in the fast method
- **WHEN** the current move is a counter-clockwise turn of the top (U') in the fast method
- **THEN** it reads "Turn the top layer to the right"

#### Scenario: Back move in the fast method
- **WHEN** the current move is a clockwise turn of the back (B) in the fast method
- **THEN** it reads "Turn the back side so its top row moves to the left"

#### Scenario: Front move in the fast method
- **WHEN** the current move is a clockwise turn of the front (F) in the fast method
- **THEN** it reads "Turn the front side clockwise"

#### Scenario: Describe a move
- **WHEN** the current move is a counter-clockwise turn of the top in the learn method or in a lesson's algorithm demo
- **THEN** it reads "Turn the top layer to the right", the same as in the fast method

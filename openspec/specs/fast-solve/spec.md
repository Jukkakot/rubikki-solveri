# fast-solve Specification

## Purpose
Gives the user a short solution for their cube and guides them through it one move at a time.

## Requirements

### Requirement: Short solution
For any valid cube the app SHALL find a solution of at most 21 face turns, about 19 on average,
in under a second on the reference phone, without blocking the screen. An already solved cube
SHALL be reported as solved; an invalid cube SHALL get the same message as the manual check.

#### Scenario: Scrambled cube
- **WHEN** a valid scrambled cube is solved
- **THEN** the moves, applied to it, solve it and there are at most 21 of them

#### Scenario: Solved cube
- **WHEN** the cube is already solved
- **THEN** the screen says it is already solved

### Requirement: Step through the solution
The solution screen SHALL show the cube as it is before the current move, the move number out of
the total with a progress bar, and the move in words. "Done" SHALL animate the move and go to the
next; "back" SHALL animate the undo of the previous move; "show" SHALL play the current move and
return the cube to before it. After the last move the screen SHALL say the cube is solved.

#### Scenario: Next and back
- **WHEN** the user taps done on move 1 of 19 and then back
- **THEN** move 2 of 19 is shown, then move 1 of 19 again with the cube as before

#### Scenario: Finished
- **WHEN** the user taps done on the last move
- **THEN** the screen says the cube is solved

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

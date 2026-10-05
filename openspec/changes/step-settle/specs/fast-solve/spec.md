# Spec Delta

## MODIFIED Requirements

### Requirement: Step through the solution
The solution screen SHALL show the cube of the current move (before it until its demo plays,
after it once the demo has played), the move number out of the total with a progress bar, and the
move in words. "Done" SHALL go to the next move without replaying the turn; "back" SHALL go to the
previous move, showing the cube as before that move, and demo it again; "show" SHALL play the
current move from before it. After the last move the screen SHALL say the cube is solved.

#### Scenario: Next and back
- **WHEN** the user taps done on move 1 of 19 and then back
- **THEN** move 2 of 19 is shown, then move 1 of 19 again with the cube as before move 1, followed by its demo

#### Scenario: Finished
- **WHEN** the user taps done on the last move
- **THEN** the screen says the cube is solved

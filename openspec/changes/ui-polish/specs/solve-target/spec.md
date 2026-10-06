# Spec Delta

## MODIFIED Requirements

### Requirement: Target on the solution screen
The start screen SHALL show the current target (its name and a small picture), solved by default,
with a way to change it. Changing the target SHALL work out a new solution from the same starting
cube to the new target and update the number of moves; the guide then starts from its first move.
When the starting cube already is the target, the start screen SHALL say so and offer to change the
target.

#### Scenario: Default target
- **WHEN** the start screen opens after a scan
- **THEN** the target shown is the solved cube and the guide leads to it as before

#### Scenario: Change the target
- **WHEN** the user picks the checkerboard as the target
- **THEN** the start screen shows the checkerboard as the target, and "Aloita" opens the guide at the first move of a solution that ends in the checkerboard

#### Scenario: Already there
- **WHEN** the starting cube is solved and the target is the solved cube
- **THEN** the start screen says the cube is already there and offers to choose a pattern

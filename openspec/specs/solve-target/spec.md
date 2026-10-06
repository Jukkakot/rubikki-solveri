# solve-target Specification

## Purpose
Lets the user choose where the real cube ends up (a pattern, a surprise, a lesson stage or a painted
cube) and leads the cube there move by move with the usual guide.

## Requirements

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

### Requirement: Reaching the target exactly
Following the guide to its end SHALL leave the real cube in exactly the target, with the centres as
they were at the start. The shortest-solution method SHALL be used for patterns and painted
targets, with a solution of at most about the length of an ordinary solution.

#### Scenario: Pattern from a scrambled cube
- **WHEN** the user follows every move from a scrambled cube to the cube-in-a-cube pattern
- **THEN** the cube shows the cube-in-a-cube pattern and the finish says the target is reached

### Requirement: Pattern gallery
The target picker SHALL list about a dozen known patterns, each with its name and a picture showing
three sides. Tapping one SHALL show it as a large cube that can be turned by dragging, with a
button to choose it.

#### Scenario: Look before choosing
- **WHEN** the user taps a pattern in the gallery
- **THEN** a large turnable cube of the pattern is shown with a choose button

### Requirement: Surprise
The target picker SHALL offer a surprise choice that picks a random pattern from the gallery, other
than the current target, and shows it as when tapped.

#### Scenario: Surprise me
- **WHEN** the user taps the surprise choice
- **THEN** a random pattern different from the current target is shown, ready to choose

### Requirement: Lesson stage as target
The target picker SHALL list the learn method's stages as targets ("stop when this stage is done"),
each with the stage's goal picture. With a stage target the guide SHALL use the learn method, with
its stage cards, and end when that stage is done.

#### Scenario: Stop after the first layer
- **WHEN** the user picks the first-layer stage as the target and follows the guide
- **THEN** the guide shows the stages up to the first layer and finishes when the first layer is done

### Requirement: Painted target
The target picker SHALL offer painting the target on the hand-input screen, starting from the
current target's colours. Only a possible cube SHALL be accepted as a target, with the usual
check messages otherwise.

#### Scenario: Paint a target
- **WHEN** the user paints a possible cube and accepts it
- **THEN** it becomes the target and the guide leads to it

#### Scenario: Impossible target
- **WHEN** the painted cube cannot exist
- **THEN** it is not accepted and the check says why

### Requirement: Patterns from a solved cube
The pattern entry on the home screen SHALL open the target picker for a cube that is solved now;
the chosen target SHALL open the solution screen from the solved cube.

#### Scenario: Make a pattern from a solved cube
- **WHEN** the user opens patterns from the home screen and chooses the checkerboard
- **THEN** the solution screen opens from the solved cube with the checkerboard as the target

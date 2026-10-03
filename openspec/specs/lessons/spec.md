# lessons Specification

## Purpose
Teaches the beginner method so the user can solve the cube without the app: explanations, the
algorithms shown on their own, and practice of one stage at a time.

## Requirements

### Requirement: Lesson list
The lessons screen SHALL list "Basics" and the seven beginner stages in order, each with its name
and a one-line summary.

#### Scenario: Open lessons
- **WHEN** the user opens the lessons from the home screen
- **THEN** Basics and stages 1–7 are listed in order

### Requirement: Lesson content
A stage lesson SHALL explain what the stage achieves, how to recognise what to do, and a tip, and
SHALL list the stage's algorithms with a name, the notation and the moves in words. Basics SHALL
explain the pieces (centres, edges, corners), that centres never move relative to each other,
the move notation, and the holding position.

#### Scenario: Middle layer lesson
- **WHEN** the user opens the middle layer lesson
- **THEN** it shows the explanation and the two algorithms U R U' R' U' F' U F and U' L' U L U F U' F'

### Requirement: Algorithm demo
Each algorithm SHALL have a 3D demo starting from a cube that the algorithm solves; "play" SHALL
animate the whole algorithm with the move guide's highlight and arrows, and the cube SHALL return
to the start afterwards.

#### Scenario: Play the trigger
- **WHEN** the user plays the R' D' R D demo
- **THEN** the four moves animate in order and the cube is solved at the end of the demo

### Requirement: Practice a stage
Practice SHALL start from a new position where all earlier stages are solved and the chosen stage
is not, guide only that stage's steps, and on finishing say the stage is done and offer a new
position.

#### Scenario: Practise the yellow cross
- **WHEN** the user practises the yellow cross
- **THEN** the position has the first two layers solved and no yellow cross, and the guide ends
  when the yellow cross is made

### Requirement: Practice count
The lesson list SHALL show how many times each stage has been practised to the end.

#### Scenario: Practised twice
- **WHEN** the yellow cross has been practised twice
- **THEN** its lesson shows "practised 2×"

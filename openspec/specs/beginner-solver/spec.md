# beginner-solver Specification

## Purpose
Solves the user's cube the way a person learns to: layer by layer with a few named algorithms,
explaining each step so the user understands what is happening and why.

## Requirements

### Requirement: Layer-by-layer solution
For any valid cube the beginner solver SHALL produce a solution in these stages, in order: white
cross, white corners, middle layer (starting by turning the cube over, yellow on top), yellow
cross, yellow edges in place, yellow corners in place, yellow corners turned. Each stage SHALL leave the earlier
stages' pieces solved.

#### Scenario: Any cube
- **WHEN** a valid scrambled cube is solved with the beginner method
- **THEN** applying all steps solves it, and after each stage that stage's pieces are solved

#### Scenario: Already solved stage
- **WHEN** a stage is already done
- **THEN** it has no steps and is reported as already done

### Requirement: Steps with explanations
Each step SHALL name what it achieves (for example which piece goes where) and why, and list its
moves. Whole-cube turns SHALL be described by which centre ends up in front and on top.

#### Scenario: Cross step
- **WHEN** a white cross step places the white–red edge
- **THEN** its explanation names the white–red edge and its place next to the red centre

#### Scenario: Whole-cube turn
- **WHEN** a step turns the whole cube so red faces the user
- **THEN** the move reads "Turn the whole cube: red centre towards you, white on top"

### Requirement: Classic algorithms
The white corners and turning the yellow corners SHALL use the trigger R' D' R D repeated; the
middle layer SHALL use U R U' R' U' F' U F and its mirror; the yellow cross F R U R' U' F'; placing
yellow edges R U R' U R U2 R' U; placing yellow corners U R U' L' U R' U' L.

#### Scenario: Middle edge
- **WHEN** a middle-layer edge is inserted to the right
- **THEN** the step's moves end with U R U' R' U' F' U F

### Requirement: Learn mode in the solution screen
The start screen SHALL let the user choose between the shortest solution and learning step by
step. In learning mode the guide SHALL show the stage number and name with a small goal picture of
the stage (tapping it shows it large), and the current step's explanation above the move guide. When
a stage begins, a card with the stage's goal picture ("Next: …") SHALL be shown until the user
continues.

#### Scenario: Choose learning
- **WHEN** the user picks "learn step by step" on the start screen and taps "Aloita"
- **THEN** stage 1/7 "White cross" is shown with its first step

#### Scenario: Next stage
- **WHEN** the last step of the white cross is done
- **THEN** a card shows the goal picture of the white corners until the user continues

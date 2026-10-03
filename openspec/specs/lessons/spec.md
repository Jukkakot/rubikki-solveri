# lessons Specification

## Purpose
Teaches the beginner method so the user can solve the cube without the app: explanations, the
algorithms shown on their own, and practice of one stage at a time.

## Requirements

### Requirement: Lesson list
The lessons screen SHALL list "Basics" and the seven beginner stages in order, each with its name,
a one-line summary and, for a stage, a small goal picture, so the list reads as the path from a
scrambled to a solved cube.

#### Scenario: Open lessons
- **WHEN** the user opens the lessons from the home screen
- **THEN** Basics and stages 1–7 are listed in order, each stage with its goal picture

### Requirement: Lesson content
A lesson SHALL be a row of pages the user moves through by swiping or with next and back buttons,
with dots showing the current page; every page SHALL fit the screen without scrolling. A stage
lesson SHALL have, in order: a goal page (the goal picture, a one-line summary and a one-line tip),
a cases page (except where the stage has no cases), one page per algorithm, and a practice page.
Basics SHALL have one page each, with a picture, for the centres, the edges and corners, a move
and its notation, and the holding position. Text on a page SHALL be at most a few short lines;
pictures carry the explanation.

#### Scenario: Middle layer lesson
- **WHEN** the user opens the middle layer lesson
- **THEN** its pages show the goal, the cases, and the two algorithms U R U' R' U' F' U F and U' L' U L U F U' F' on pages of their own

#### Scenario: Swipe through a lesson
- **WHEN** the user taps next on the goal page of a stage lesson
- **THEN** the cases page is shown and the dots mark the second page

### Requirement: Algorithm demo
Each algorithm SHALL have a 3D demo starting from a cube that the algorithm solves; "play" SHALL
animate the whole algorithm with the move guide's highlight and arrows, and the cube SHALL return
to the start afterwards. While playing, the current move SHALL be highlighted in the notation and
described in words. The page SHALL show small before and after pictures with the pieces the
algorithm moves outlined.

#### Scenario: Play the trigger
- **WHEN** the user plays the R' D' R D demo
- **THEN** the four moves animate in order and the cube is solved at the end of the demo

#### Scenario: Current move
- **WHEN** the second move of the trigger is playing
- **THEN** D' is highlighted in the notation and its description in words is shown

#### Scenario: What the algorithm moves
- **WHEN** the yellow cross algorithm page is shown
- **THEN** the before and after pictures outline the pieces that F R U R' U' F' moves

### Requirement: Practice a stage
Practice SHALL start from a new position where all earlier stages are solved and the chosen stage
is not, first show the stage's goal picture, guide only that stage's steps, and on finishing say
the stage is done and offer a new position.

#### Scenario: Practise the yellow cross
- **WHEN** the user practises the yellow cross
- **THEN** the position has the first two layers solved and no yellow cross, and the guide ends
  when the yellow cross is made

#### Scenario: Goal first
- **WHEN** a practice position opens
- **THEN** the stage's goal picture is shown until the user continues to the moves

### Requirement: Practice count
The lesson list SHALL show how many times each stage has been practised to the end.

#### Scenario: Practised twice
- **WHEN** the yellow cross has been practised twice
- **THEN** its lesson shows "practised 2×"

### Requirement: Stage goal picture
Each stage SHALL have a goal picture: the 3D cube as it looks when the stage is done, held as the
method holds it during the stage, with every sticker not yet in place shown grey and the stickers
this stage puts in place outlined. Where a stage places pieces without fixing their twist, the
picture SHALL say so in one short line. The goal picture SHALL be turnable by dragging wherever it
is shown large.

#### Scenario: White cross goal
- **WHEN** the goal picture of the white cross is shown
- **THEN** the white centre, the four white edges and the side centres are coloured, the four edges are outlined and every other sticker is grey

#### Scenario: Middle layer goal
- **WHEN** the goal picture of the middle layer is shown
- **THEN** the cube is held yellow on top, the first two layers are coloured, the four middle edges are outlined and the yellow layer is grey apart from its centre

### Requirement: Case pictures
Each stage lesson except the white cross SHALL show the stage's typical situations as pictures:
a small cube in that situation with the relevant piece highlighted, a one-line caption and what to
do (the algorithm and how many times, or the turn to make). Tapping a case SHALL play its moves on
a large cube. Doing a case's moves from its pictured position SHALL reach what the caption
promises. The white cross SHALL show its situations (edge down, edge in the middle layer, edge on
top the wrong way) with the turns that bring the edge up.

#### Scenario: Yellow cross cases
- **WHEN** the user opens the cases page of the yellow cross
- **THEN** the dot, the L and the line are shown, each with how to hold it and F R U R' U' F'

#### Scenario: Play a case
- **WHEN** the user taps the "white to the right" case of the white corners
- **THEN** the case's moves play on a large cube and the corner ends in place

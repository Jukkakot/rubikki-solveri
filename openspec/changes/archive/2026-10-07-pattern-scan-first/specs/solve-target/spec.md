## REMOVED Requirements

### Requirement: Patterns from a solved cube
**Reason**: Replaced by "Patterns from home", which also lets the user scan a cube that is not solved.
**Migration**: The solved-cube path stays as one of the two choices.

## ADDED Requirements

### Requirement: Patterns from home
The pattern entry on the home screen SHALL open the target picker. Choosing a target there SHALL
ask where the cube starts, with two choices: scan the cube (the primary choice) or the cube is
already solved. Scanning SHALL open the usual scan and, once the scan is accepted, the solution's
start screen from the scanned cube with the chosen target; the colour check and hand input reached
from that scan SHALL keep the target. The scan screens SHALL NOT show the target. The solved choice
SHALL open the solution's start screen from the solved cube. Dismissing the question SHALL leave
the user in the picker. The question SHALL NOT remember the last choice.

#### Scenario: Scan first
- **WHEN** the user opens patterns from home, chooses the checkerboard, picks "scan the cube" and scans a scrambled cube
- **THEN** the solution's start screen opens from the scanned cube with the checkerboard as the target

#### Scenario: Through the colour check
- **WHEN** the scan in this flow needs the colour check and the user accepts the colours there
- **THEN** the solution's start screen opens with the chosen target, not the solved cube

#### Scenario: Already solved
- **WHEN** the user opens patterns from home, chooses the checkerboard and picks "already solved"
- **THEN** the solution's start screen opens from the solved cube with the checkerboard as the target

#### Scenario: Change of mind
- **WHEN** the user dismisses the start question
- **THEN** the picker stays open and nothing is chosen

## MODIFIED Requirements

### Requirement: Painted target
The target picker SHALL offer painting the target on the hand-input screen, starting from the
current target's colours. Only a possible cube SHALL be accepted as a target, with the usual
check messages otherwise. A painted target SHALL fit any starting cube however it is held: it is
turned as a whole so that its centres match the starting cube's, and the guide leads to that.

#### Scenario: Paint a target
- **WHEN** the user paints a possible cube and accepts it
- **THEN** it becomes the target and the guide leads to it

#### Scenario: Impossible target
- **WHEN** the painted cube cannot exist
- **THEN** it is not accepted and the check says why

#### Scenario: Cube held another way
- **WHEN** the user paints a target from home, then scans a cube held with other centres up and in front
- **THEN** the guide leads to the painted pattern turned to match how the scanned cube is held

## REMOVED Requirements

### Requirement: Patterns from a solved cube
**Reason**: Replaced by "Patterns from home", which also lets the user scan a cube that is not solved.
**Migration**: The solved-cube path stays as one of the two choices.

## ADDED Requirements

### Requirement: Patterns from home
The pattern entry on the home screen SHALL open the target picker. Choosing a target there SHALL
ask where the cube starts, with two choices: scan the cube (the primary choice) or the cube is
already solved. Scanning SHALL open the usual scan and, once the scan is accepted, the solution
screen from the scanned cube with the chosen target; the colour check and hand input reached from
that scan SHALL keep the target. The solved choice SHALL open the solution screen from the solved
cube. Dismissing the question SHALL leave the user in the picker.

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

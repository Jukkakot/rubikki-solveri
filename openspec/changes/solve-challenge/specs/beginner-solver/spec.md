## ADDED Requirements

### Requirement: Try it yourself
The start screen SHALL offer a third way beside the shortest solution and learning step by step: trying each stage
of the beginner method by oneself. For each stage the screen SHALL show the stage's name, a one-line aim and the
goal as a turnable 3D cube (the user's cube with the stage done, pieces the stage does not place shown grey). A hint
SHALL show the stage's next move from the cube as last checked with the move guide, and count it. Checking SHALL
read the real cube with the video scan: when the stage and the earlier ones are done the next stage SHALL begin,
worked out from the read cube; otherwise the goal SHALL be shown again with the pieces still out of place marked.
When the cube is solved, the finish SHALL show the number of hints.

#### Scenario: Stage tried alone
- **WHEN** the user chooses "Kokeile itse" and starts
- **THEN** the white cross is shown as the goal on a turnable cube, with a hint and a check button and no moves shown

#### Scenario: Hint
- **WHEN** the user taps the hint during the white cross
- **THEN** the next move of the white cross is shown with the move guide and the hint count goes up by one

#### Scenario: Checked done
- **WHEN** the user has made the white cross and checks with the video scan
- **THEN** the white corners begin as the next goal, worked out from the scanned cube

#### Scenario: Checked not yet
- **WHEN** the scanned cube does not have the white cross yet
- **THEN** the goal is shown again with the white edges out of place marked, and the hint works from the scanned cube

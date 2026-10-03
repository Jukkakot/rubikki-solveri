## ADDED Requirements

### Requirement: One screen
In portrait the scan screen SHALL fit the display without scrolling. The actions (capture and redo
while scanning; scan again and go on during the review) SHALL stay visible at the bottom, and the
status line, the hold progress and the review texts SHALL be shown on the camera view, not between
it and the actions.

#### Scenario: Review on a phone
- **WHEN** a face has been captured on a phone in portrait
- **THEN** "Good, next" and "Scan again" are visible without scrolling, and the review texts are on the dimmed camera view

#### Scenario: Scanning on a phone
- **WHEN** a face is being held in the grid
- **THEN** the status and the progress are shown on the camera view and the capture button is visible without scrolling

## MODIFIED Requirements

### Requirement: Capture
A face SHALL be captured automatically when the readings stay the same with the right centre for
about 1.5 seconds, with a short vibration, or at once with the capture button. While the right face
is held steady, a progress indicator SHALL show how much of the hold time has passed; any change
in the readings SHALL restart it. The previous face SHALL be re-scannable.

#### Scenario: Held still
- **WHEN** the right face is held still in the grid for the hold time
- **THEN** it is captured and shown for confirmation

#### Scenario: Still turning
- **WHEN** the right centre is seen but the other readings change within the hold time
- **THEN** nothing is captured and the progress starts again

#### Scenario: Redo
- **WHEN** the user taps redo after confirming the right face
- **THEN** the right face is asked for again

## ADDED Requirements

### Requirement: Confirm each face
After a capture the screen SHALL stop reading the camera and show the nine colours as read, with a
choice to accept them or scan the same face again. Only an accepted face SHALL count as done.

#### Scenario: Accept
- **WHEN** the front face has been captured and the user taps "Looks right"
- **THEN** the right face is asked for

#### Scenario: Scan again
- **WHEN** the front face has been captured and the user taps "Scan again"
- **THEN** the front face is asked for again and nothing is stored for it

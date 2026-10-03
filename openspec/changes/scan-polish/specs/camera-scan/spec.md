## MODIFIED Requirements

### Requirement: Steady camera settings
When the first face is captured, the camera's exposure and white balance SHALL be locked for the
rest of the scan, so that every face is read under the settings the camera had while the front face
was held still. Scanning the front face again SHALL release the lock until it is captured again.

#### Scenario: Lock after the first face
- **WHEN** the front face is captured
- **THEN** exposure and white balance are locked until the scan ends or returns to the front face

#### Scenario: Turning after the first face
- **WHEN** the front face has been captured and the user turns the cube towards a darker view before accepting
- **THEN** the next faces are read with the front face's exposure, not a brighter one

## ADDED Requirements

### Requirement: Smooth capture
The camera view SHALL keep moving through a capture and the review; saving the capture's picture
SHALL not hold up the screen. A stop of the camera frames or of the screen's drawing long enough to
notice SHALL be written to the log with where it happened and how long it lasted.

#### Scenario: Capture without a freeze
- **WHEN** a face is captured
- **THEN** the camera view does not freeze while the picture is saved

#### Scenario: Stall logged
- **WHEN** no camera frame arrives for a noticeable time during the scan
- **THEN** the log gets a stall line with its length

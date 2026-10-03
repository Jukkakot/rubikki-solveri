## ADDED Requirements

### Requirement: Scan pictures
Each capture in the scan SHALL save a small picture of the grid area on the phone, and the log line
of that capture SHALL name the picture. Only the newest twelve pictures SHALL be kept. Sharing the
log SHALL send the pictures together with the log file, and clearing the log SHALL delete them.
Pictures SHALL never leave the phone except when the user shares the log.

#### Scenario: Picture of a capture
- **WHEN** a face is captured
- **THEN** a picture of the grid area is saved and the capture's log line names it

#### Scenario: Share with pictures
- **WHEN** the user shares the log after a scan
- **THEN** the share sheet gets the log file and the scan pictures

#### Scenario: Clear removes pictures
- **WHEN** the user clears the log
- **THEN** the scan pictures are deleted too

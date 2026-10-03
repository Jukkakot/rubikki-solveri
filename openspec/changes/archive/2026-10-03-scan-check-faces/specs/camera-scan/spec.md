## ADDED Requirements

### Requirement: Scan one face
The scan SHALL be able to ask for a single face, started from the check of a scan. It SHALL show that
face with how to hold the cube, capture and confirm it as in the full scan, and on "Good, next"
return the face's readings and its picture to the check instead of asking for another face. The
face progress SHALL show only that face, and the action to scan the previous face again SHALL not be
offered.

#### Scenario: One face only
- **WHEN** the one-face scan is opened for the top face and the user accepts a capture
- **THEN** the check is shown again with the top face's new reading and no other face is asked for

#### Scenario: Scan the same face again
- **WHEN** the top face has been captured in the one-face scan and the user taps "Scan again"
- **THEN** the top face is asked for again

## MODIFIED Requirements

### Requirement: Reading stays quick in a long scan
The scan's work per picture SHALL not grow with how long the scan has run: faces seen long ago SHALL
count by what they showed without being worked through again in every picture, so late in a long
scan as many pictures a second are read as at its start.
The busiest moments (two faces in view, the cube about to be clear) SHALL NOT cost much more than
the rest: the scan SHALL work out how sure each sticker is only as far as its decisions need. In the
browser, finding the faces in a picture and the scan's own work SHALL run side by side, so the
pictures a second are set by the slower of the two, not by both together.

#### Scenario: A minute of turning
- **WHEN** the user has turned the cube in front of the camera for a minute without the scan finishing
- **THEN** the log's snapshots show about as many pictures a second as in the first seconds

#### Scenario: Two faces in view
- **WHEN** the user holds the cube with two faces in view in the browser, just before the scan finishes
- **THEN** the log's snapshots show about as many pictures a second as while one face was in view

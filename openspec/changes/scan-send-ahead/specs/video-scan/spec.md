## MODIFIED Requirements

### Requirement: Reading stays quick in a long scan
The scan's work per picture SHALL not grow with how long the scan has run: faces seen long ago SHALL
count by what they showed without being worked through again in every picture, so late in a long
scan as many pictures a second are read as at its start.
The busiest moments (two faces in view, the cube about to be clear) SHALL NOT cost much more than
the rest: the scan SHALL work out how sure each sticker is only as far as its decisions need. In the
browser, finding the faces in a picture and the scan's own work SHALL run side by side, so the
pictures a second are set by the slower of the two, not by both together.
In the browser, the next picture SHALL be made ready while the faces of the current one are being
found, so finding faces does not wait for a picture whenever the camera has a new one; each camera
picture SHALL be read at most once, and at most one picture SHALL wait to be read (a newer one takes
its place).

#### Scenario: A minute of turning
- **WHEN** the user has turned the cube in front of the camera for a minute without the scan finishing
- **THEN** the log's snapshots show about as many pictures a second as in the first seconds

#### Scenario: Two faces in view
- **WHEN** the user holds the cube with two faces in view in the browser, just before the scan finishes
- **THEN** the log's snapshots show about as many pictures a second as while one face was in view

#### Scenario: Finder kept busy
- **WHEN** the video scan runs in the phone's browser and finding faces takes clearly less time than the camera's picture interval
- **THEN** the log's snapshots show about as many pictures a second as the camera gives, and the finder's wait between pictures is short

#### Scenario: No picture twice
- **WHEN** the video scan runs in the browser and finding faces is faster than the camera
- **THEN** the pictures read a second do not exceed the camera's own rate

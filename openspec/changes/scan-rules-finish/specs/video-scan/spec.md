## MODIFIED Requirements

### Requirement: Finish the video scan
The scan SHALL finish when one possible cube fits what has been read clearly better than every other
possible cube, including every other way of telling which reading showed which face, and this holds
for about half a second; not every sticker needs to have been seen. A face newly in view that is not
told yet SHALL not hold the finish back while it reads like that cube; only a face that reads
against it, or the cube becoming unclear, SHALL. It SHALL never finish with a
cube that breaks a rule of a real cube. The solution's start screen SHALL then open with a new scan
behind it, as for a sure guided scan; stickers known only from the others are marked in the colour
check reached from the guide's menu. The user SHALL be able to stop earlier and open the check with
what is known. The scan SHALL never stay with everything read and nothing happening.

#### Scenario: Whole cube seen
- **WHEN** the cube that fits the readings is clear for half a second
- **THEN** the start screen opens, and going back starts a new scan

#### Scenario: Cube still turning when it is clear
- **WHEN** the cube is clear and the user keeps turning it, so new faces come into view every moment
- **THEN** the scan finishes about half a second after the cube became clear

#### Scenario: Orange read as red
- **WHEN** one orange sticker has been read as red, so no real piece fits it
- **THEN** the scan takes the piece that fits the rest and finishes

#### Scenario: Two ways to tell the faces
- **WHEN** the readings fit two cubes about equally, because two faces could be told apart either way
- **THEN** the scan does not finish until a view settles it, and meanwhile the status line asks to turn the cube

#### Scenario: Stop early
- **WHEN** the user stops the video scan with stickers still unknown
- **THEN** the check opens with the known colours and the unknown stickers marked

## ADDED Requirements

### Requirement: Reading stays quick in a long scan
The scan's work per picture SHALL not grow with how long the scan has run: faces seen long ago SHALL
count by what they showed without being worked through again in every picture, so late in a long
scan as many pictures a second are read as at its start.

#### Scenario: A minute of turning
- **WHEN** the user has turned the cube in front of the camera for a minute without the scan finishing
- **THEN** the log's snapshots show about as many pictures a second as in the first seconds

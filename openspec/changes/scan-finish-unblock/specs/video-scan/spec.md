## MODIFIED Requirements

### Requirement: Finish the video scan
The scan SHALL finish when one possible cube fits what has been read clearly better than every other
possible cube, including every other way of telling which reading showed which face, and this holds
for about half a second; not every sticker needs to have been seen. A followed face whose face or
turn is not told yet SHALL not hold the finish back while it reads like that cube in some turn of a
face it could be. A followed face that reads against the clear cube SHALL hold it back only while its
readings weigh about as much as those that agree; when the same side's readings that fit the cube
clearly outnumber it, it counts as a misread and does not hold the finish. It SHALL never finish with
a cube that breaks a rule of a real cube. The solution's start screen SHALL then open with a new scan
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
- **THEN** the scan does not finish until a view settles it, and meanwhile the status line names a side that settles it

#### Scenario: Old face with an open turn
- **WHEN** the cube has been clear for a while and an early followed face of the white side, whose turn was never told, reads like the white side of that cube in one of its turns
- **THEN** it does not hold the finish back, and the scan finishes

#### Scenario: Outvoted misread
- **WHEN** the cube is clear and one short followed face of the white side reads two stickers against it, while the white side's other followed faces fit it with several times as many readings
- **THEN** that face counts as a misread and the scan finishes

#### Scenario: Stop early
- **WHEN** the user stops the video scan with stickers still unknown
- **THEN** the check opens with the known colours and the unknown stickers marked

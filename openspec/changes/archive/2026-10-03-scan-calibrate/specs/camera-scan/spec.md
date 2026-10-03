## MODIFIED Requirements

### Requirement: Live reading
While a face is in the grid, each cell SHALL show the colour it currently reads, compared with the
colours this cube has already shown (accepted centres and the user's corrections) and with a
default palette for colours not seen yet. If the centre does not match the face asked for, the
screen SHALL say which centre to turn towards the camera. A centre SHALL count as matching also
when the asked colour is the second closest after a colour the cube has not shown yet.

#### Scenario: Wrong face
- **WHEN** the front face is asked for and the camera sees a red centre
- **THEN** the screen says to turn the green centre towards the camera and does not capture

#### Scenario: Warm red
- **WHEN** the cube's red reads closer to the default orange and the right face (red centre) is asked for
- **THEN** the face is captured, and later faces read this cube's red as red

### Requirement: Confirm each face
After a capture the screen SHALL stop reading the camera and show the nine colours as read, with a
choice to accept them or scan the same face again. Tapping a sticker other than the centre SHALL
change it to the next closest colour; an accepted correction SHALL keep its colour in the result
and SHALL be used as a reference for the following faces. Only an accepted face SHALL count as done.

#### Scenario: Accept
- **WHEN** the front face has been captured and the user taps "Looks right"
- **THEN** the right face is asked for

#### Scenario: Scan again
- **WHEN** the front face has been captured and the user taps "Scan again"
- **THEN** the front face is asked for again and nothing is stored for it

#### Scenario: Tap to fix
- **WHEN** a red sticker is shown as orange and the user taps it
- **THEN** it turns red, and the same red on the next faces reads as red

## ADDED Requirements

### Requirement: Steady camera settings
After the first face is accepted, the camera's exposure and white balance SHALL stay fixed for the
rest of the scan, so that every face is read under the same settings.

#### Scenario: Lock after the first face
- **WHEN** the front face is accepted
- **THEN** exposure and white balance are locked until the scan ends or returns to the front face

## MODIFIED Requirements

### Requirement: Scan from video
The video scan SHALL read the cube from the camera's live picture without a grid, without holding
still and without separate captures. It SHALL find the cube's faces anywhere in the picture,
straight on or at an angle, several at a time, and outline each found face on the camera picture.
A face with one or two stickers hidden (for example under a finger) SHALL still count for the
stickers it shows.

#### Scenario: Turning the cube
- **WHEN** the user turns a cube slowly in front of the camera so that every face is seen
- **THEN** the stickers are recognised without any tap

#### Scenario: Corner view
- **WHEN** the cube is held so that three faces are seen at once
- **THEN** all three faces are outlined and read

#### Scenario: Finger over a sticker
- **WHEN** a face is seen with a finger over one of its edge stickers
- **THEN** its other eight stickers count towards recognition and the hidden one does not

### Requirement: Progress cube
The screen SHALL show, small in a corner of the camera picture, a 3D cube that starts all grey.
A sticker with readings that is not yet recognised SHALL show its leading colour faintly; a
recognised sticker SHALL show its colour fully. Once the pose is known the progress cube SHALL turn
with the real cube in real time, following also how much the cube is turned or tilted between two
poses, smoothly and without jitter; while no face is seen it SHALL keep its last position.
Stickers that contradict the rest SHALL be marked on it. A small vibration SHALL tell when new
stickers are recognised.

#### Scenario: Filling in
- **WHEN** a face has been read a first time
- **THEN** its stickers on the progress cube show their colours faintly, and fully once recognised

#### Scenario: Same pose
- **WHEN** the user holds the real cube with the yellow centre towards the camera and the pose is known
- **THEN** the progress cube shows its yellow side towards the user too

#### Scenario: Turning slowly
- **WHEN** the user slowly turns the real cube a quarter turn about the camera's axis
- **THEN** the progress cube turns along with it during the turn, not only at the end

#### Scenario: Cube out of view
- **WHEN** the cube leaves the camera picture
- **THEN** the progress cube stays as it was last seen

## ADDED Requirements

### Requirement: Colours on the camera picture
Every sticker of a face found in the camera picture SHALL be marked on the picture with the colour
it was read as in that frame, so the user sees what the app thinks each sticker is. The mark SHALL
be smaller than the sticker so the real sticker stays visible around it. A sticker already
recognised SHALL have a solid mark, one not yet recognised a faint mark.

#### Scenario: Reading shown
- **WHEN** a face is found in the picture
- **THEN** each of its stickers shows a mark in the colour it was read as

#### Scenario: Misread visible
- **WHEN** a sticker is read as a different colour than it really is
- **THEN** its mark shows the wrong colour against the real sticker around it

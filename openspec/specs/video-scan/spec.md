# video-scan Specification

## Purpose

Scanning a cube by turning it freely in front of the camera: the app recognises the stickers bit
by bit from the video, shows the progress on a 3D cube and hints how to turn the cube next.

## Requirements

### Requirement: Scan from video
The video scan SHALL read the cube from the camera's live picture without a grid, without holding
still and without separate captures. It SHALL find the cube's faces anywhere in the picture,
straight on or at an angle, several at a time, and outline each found face on the camera picture
with a dim line. A face with one or two stickers hidden (for example under a finger) SHALL still count for the
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

### Requirement: Turning hints
While stickers are missing, the screen SHALL show how to turn the cube to bring them into view: an
arrow on the progress cube (not over the camera picture) and one short line of text. No hint SHALL be shown while no face is
found or the pose is not known.

#### Scenario: Show the missing side
- **WHEN** only the bottom face is still unrecognised
- **THEN** the arrow and the text ask the user to tilt the cube so the bottom comes into view

### Requirement: Recognised by agreement
A sticker SHALL count as recognised only when several frames agree on its colour; a single wrong
frame SHALL NOT change a recognised sticker or finish the scan.

#### Scenario: One bad frame
- **WHEN** one frame reads a sticker wrong among many that read it right
- **THEN** the sticker keeps its right colour

### Requirement: Finish the video scan
When all 54 stickers are recognised and the cube is possible, and this holds for about half a
second, the solution SHALL open, with the colour check behind it as for a sure guided scan. The
user SHALL be able to stop earlier and open the check with what is known.

#### Scenario: Whole cube seen
- **WHEN** every sticker is recognised and the cube is possible for half a second
- **THEN** the solution opens and going back shows the check

#### Scenario: Stop early
- **WHEN** the user stops the video scan with stickers still missing
- **THEN** the check opens with the known colours and the missing stickers marked

### Requirement: Guided scan stays
The guided scan SHALL stay available. Until the video scan is reliable it SHALL be the default, with
the video scan offered beside it; afterwards the video scan SHALL be the default and the guided
scan offered beside it.

#### Scenario: Choosing the way
- **WHEN** the user opens scanning
- **THEN** both ways are offered, the current default first

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

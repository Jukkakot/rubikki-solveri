# video-scan Specification

## Purpose

Scanning a cube by turning it freely in front of the camera: the app recognises the stickers bit
by bit from the video, shows the progress on a 3D cube and hints how to turn the cube next.

## Requirements

### Requirement: Scan from video
The video scan SHALL read the cube from the camera's live picture without a grid, without holding
still and without separate captures. It SHALL find the cube's faces anywhere in the picture,
straight on or at an angle, several at a time, and outline each found face on the camera picture.

#### Scenario: Turning the cube
- **WHEN** the user turns a cube slowly in front of the camera so that every face is seen
- **THEN** the stickers are recognised without any tap

#### Scenario: Corner view
- **WHEN** the cube is held so that three faces are seen at once
- **THEN** all three faces are outlined and read

### Requirement: Progress cube
The screen SHALL show, small in a corner of the camera picture, a 3D cube that starts all grey and fills in each sticker with its colour once
that sticker is recognised. It SHALL turn to the pose the real cube is held in once that pose is
known. Stickers that contradict the rest SHALL be marked on it. A small vibration SHALL tell when
new stickers are recognised.

#### Scenario: Filling in
- **WHEN** a face has been recognised
- **THEN** its stickers on the progress cube change from grey to their colours

#### Scenario: Same pose
- **WHEN** the user holds the real cube with the yellow centre towards the camera and the pose is known
- **THEN** the progress cube shows its yellow side towards the user too

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

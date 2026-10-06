# Spec Delta

## MODIFIED Requirements

### Requirement: Guided scan stays
The guided scan SHALL stay available. The video scan SHALL be the default: every new scan SHALL
start as the video scan. The video scan screen's menu SHALL offer an entry that switches to the
guided scan, and the guided scan screen a button that switches back to the video scan; switching
SHALL replace the screen, so going back leads to where the scan was started from. Rescanning a
single face from the colour check SHALL use the guided scan.

#### Scenario: Choosing the way
- **WHEN** the user starts scanning from the home screen
- **THEN** the video scan opens

#### Scenario: Switch to one face at a time
- **WHEN** the user picks "one picture at a time" from the video scan's menu
- **THEN** the guided scan opens in its place, and going back returns to the home screen

#### Scenario: Switch back to video
- **WHEN** the user taps "video" on the guided scan
- **THEN** the video scan opens in its place

#### Scenario: Rescan one face
- **WHEN** the user rescans one face from the colour check
- **THEN** the guided scan opens for that face only

### Requirement: Colours on the camera picture
Every sticker of a face found in the camera picture SHALL be painted on the picture as a tile in the
colour it was read as in that frame, so the user sees what the app thinks each sticker is. The tile
SHALL be smaller than the sticker so the real sticker stays visible around it. A known sticker SHALL
have a solid tile, one still needed a grey dashed tile.

#### Scenario: Reading shown
- **WHEN** a face is found in the picture
- **THEN** each of its stickers shows a tile in the colour it was read as

#### Scenario: Misread visible
- **WHEN** a sticker is read as a different colour than it really is
- **THEN** its tile shows the wrong colour against the real sticker around it

### Requirement: Progress on the real cube
The screen SHALL show the progress on the real cube in the camera picture, not on a separate cube.
Every sticker on a side of the real cube turned towards the camera SHALL be painted: a solid tile
in its colour when known, a grey dashed tile when still needed, so the grey parts show what is left
to show. A side SHALL get a white outline only when the rest of the cube confirms all its stickers,
not on its own readings alone. When the cube's pose cannot be told, at least the faces found SHALL
be painted. A small vibration SHALL tell when new stickers become known.

#### Scenario: Filling in
- **WHEN** a side is shown to the camera and its stickers become known
- **THEN** their grey tiles turn into solid tiles in their colours, and the side gets an outline once the rest of the cube confirms it

#### Scenario: Sure only with the rest of the cube
- **WHEN** a side has been read many times the same way but the rest of the cube does not confirm it yet
- **THEN** its stickers show solid tiles but the side has no outline

#### Scenario: Side at an angle
- **WHEN** the cube is held so that a side is seen at an angle too steep to read
- **THEN** that side's stickers are still painted as known or still needed

#### Scenario: Sides done at a glance
- **WHEN** the white and the green side are confirmed and both are turned towards the camera
- **THEN** both have an outline and their stickers are painted, and no row of side colours is shown under the picture

### Requirement: Camera set for the cube
During the video scan the camera SHALL measure the light and focus where the cube is, not on the
whole picture, once a face is found. While the stickers read washed out (too bright to tell their
colours), the camera SHALL be made darker step by step; exposure and white balance SHALL be locked
only once the stickers read well, or when the camera can be made no darker. Turning the torch on or
off SHALL go through the same steps again. Every picture the camera delivers SHALL be offered for
reading, with no limit of the app's own; a picture that arrives while the previous one is still
being read SHALL be dropped, so reading never falls behind the camera. A browser that cannot read
off the page's thread SHALL keep about fifteen a second, so the page stays responsive.

#### Scenario: Torch in a dark room
- **WHEN** the user scans in a dark room with the torch on and the cube fills only part of the picture
- **THEN** the stickers are not washed out once the scan has settled, and red and orange are told apart

#### Scenario: Cube moved to another part of the picture
- **WHEN** the cube is first found at the edge of the picture and then held in the middle
- **THEN** the camera stays focused on the cube and the readings stay sharp

#### Scenario: Camera that cannot be made darker
- **WHEN** the camera offers no way to lower its exposure
- **THEN** the scan locks as before and goes on, and washed-out readings count little

#### Scenario: As fast as the phone allows
- **WHEN** the camera delivers 30 pictures a second and each is read in less than a thirtieth of a second
- **THEN** about 30 pictures a second are read, as the log's snapshot shows

## ADDED Requirements

### Requirement: Paint follows the cube
The paint SHALL move smoothly with the real cube between readings, without jumps, and SHALL stay on
the cube when a reading misses its pose for a moment. When no face has been found for about a
second, the paint SHALL fade out. It SHALL come back as soon as the cube is seen again.

#### Scenario: Turning the cube
- **WHEN** the user turns the cube slowly in front of the camera
- **THEN** the tiles glide with the stickers and do not flicker or jump between positions

#### Scenario: Pose missed for a moment
- **WHEN** a few pictures in a row give no pose while the cube stays in view
- **THEN** the tiles stay on the cube

#### Scenario: Cube out of view
- **WHEN** the cube is taken out of the picture
- **THEN** the paint fades out within about a second, and appears again when the cube is back

### Requirement: Progress ring
A small ring at the top of the picture SHALL fill with the share of the cube's stickers that are
known, without a number. It SHALL be full when the scan finishes, also when the scan finishes with
some stickers never seen.

#### Scenario: Half known
- **WHEN** 27 of the 54 stickers are known
- **THEN** the ring is half full

#### Scenario: Finish before every sticker is seen
- **WHEN** the scan finishes with 50 stickers known
- **THEN** the ring is full

### Requirement: Scan screen layout
The camera picture SHALL fill the screen, with no title and no buttons below it. On the picture
there SHALL be round icon buttons for back, the torch (where the device has one) and a menu. The
menu SHALL offer one picture at a time, entering the colours by hand, and the colour check with
what is known. The dim-light and stall notices SHALL stay as before.

#### Scenario: Nothing below the picture
- **WHEN** the video scan opens
- **THEN** the camera fills the screen with the back, torch and menu icons on it and no buttons below it

#### Scenario: Stop early from the menu
- **WHEN** the user picks the colour check from the menu with stickers still unknown
- **THEN** the check opens with the known colours and the unknown stickers marked

### Requirement: One status line
One short status line SHALL lie at the bottom of the picture. It SHALL ask to show the cube when
none is found, ask to show the grey parts while stickers are needed, and say the scan is ready at
the end. A stall notice SHALL take its place while shown.

#### Scenario: Status line
- **WHEN** a cube is in view and stickers are still needed
- **THEN** the line asks to show the grey parts

#### Scenario: No cube
- **WHEN** no face is found in the picture
- **THEN** the line asks to show the cube to the camera

## REMOVED Requirements

### Requirement: Turning hints
**Reason**: The arrow appeared only while a face with a settled pose was in view, so it came and went
and its meaning was unclear (user, 2026-10-06). The grey tiles show what is left to show.
**Migration**: None; the grey paint on the cube ("Progress on the real cube") takes its place.

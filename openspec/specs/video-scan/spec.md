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

### Requirement: Turning hints
While stickers are still needed, a large arrow on the camera picture beside the real cube SHALL
show which way to turn it to bring them into view, with at most a few words. No arrow SHALL be
shown while no face is found or the pose is not known.

#### Scenario: Show the missing side
- **WHEN** only the bottom side still has stickers needed
- **THEN** the arrow asks the user to tilt the cube so the bottom comes into view

### Requirement: Recognised by agreement
A sticker SHALL count as known only when several frames agree on its colour, or when the rest of
the cube leaves only one possible colour for it. A single wrong frame SHALL NOT change a known
sticker or finish the scan. Red and orange readings SHALL count as weaker evidence against each
other than other colours.

#### Scenario: One bad frame
- **WHEN** one frame reads a sticker wrong among many that read it right
- **THEN** the sticker keeps its right colour

#### Scenario: Known from the others
- **WHEN** two stickers of a corner are known and its third was never seen
- **THEN** the third is known from the corner's colours

### Requirement: Finish the video scan
The scan SHALL finish when one possible cube fits what has been read clearly better than any other
possible cube, and this holds for about half a second; not every sticker needs to have been seen.
The solution's start screen SHALL then open with a new scan behind it, as for a sure guided scan;
stickers known only from the others are marked in the colour check reached from the guide's menu.
The user SHALL be able to stop earlier and open the check with what is known. The scan SHALL never
stay with everything read and nothing happening.

#### Scenario: Whole cube seen
- **WHEN** the cube that fits the readings is clear for half a second
- **THEN** the start screen opens, and going back starts a new scan

#### Scenario: Orange read as red
- **WHEN** one orange sticker has been read as red, so no real piece fits it
- **THEN** the scan takes the piece that fits the rest and finishes

#### Scenario: Stop early
- **WHEN** the user stops the video scan with stickers still unknown
- **THEN** the check opens with the known colours and the unknown stickers marked

### Requirement: Guided scan stays
The guided scan SHALL stay available. The video scan SHALL be the default: every new scan SHALL
start as the video scan. The video scan screen SHALL offer a button that switches to the guided
scan, and the guided scan screen a button that switches back to the video scan; switching SHALL
replace the screen, so going back leads to where the scan was started from. Rescanning a single
face from the colour check SHALL use the guided scan.

#### Scenario: Choosing the way
- **WHEN** the user starts scanning from the home screen
- **THEN** the video scan opens

#### Scenario: Switch to one face at a time
- **WHEN** the user taps "one picture at a time" on the video scan
- **THEN** the guided scan opens in its place, and going back returns to the home screen

#### Scenario: Switch back to video
- **WHEN** the user taps "video" on the guided scan
- **THEN** the video scan opens in its place

#### Scenario: Rescan one face
- **WHEN** the user rescans one face from the colour check
- **THEN** the guided scan opens for that face only

### Requirement: Colours on the camera picture
Every sticker of a face found in the camera picture SHALL be marked on the picture with the colour
it was read as in that frame, so the user sees what the app thinks each sticker is. The mark SHALL
be smaller than the sticker so the real sticker stays visible around it. A known sticker SHALL have
a solid mark, one still needed an empty mark.

#### Scenario: Reading shown
- **WHEN** a face is found in the picture
- **THEN** each of its stickers shows a mark in the colour it was read as

#### Scenario: Misread visible
- **WHEN** a sticker is read as a different colour than it really is
- **THEN** its mark shows the wrong colour against the real sticker around it

### Requirement: Progress on the real cube
The screen SHALL show the progress on the real cube in the camera picture, not on a separate cube.
Every sticker on a side of the real cube turned towards the camera SHALL be marked: a solid mark in
its colour when known, an empty mark when still needed. A side SHALL carry a tick only when the
rest of the cube confirms all its stickers, not on its own readings alone. When the cube's pose
cannot be told, at least the faces found SHALL be marked. A small vibration SHALL tell when new
stickers become known. Below the picture a row of the six side colours SHALL show which sides are
confirmed in the same way, so the progress is visible also while the cube is out of view.

#### Scenario: Filling in
- **WHEN** a side is shown to the camera and its stickers become known
- **THEN** their empty marks turn into solid marks in their colours, and a tick appears once the rest of the cube confirms the side

#### Scenario: Sure only with the rest of the cube
- **WHEN** a side has been read many times the same way but the rest of the cube does not confirm it yet
- **THEN** its stickers show solid marks but the side has no tick

#### Scenario: Sides done at a glance
- **WHEN** the white and the green side are confirmed and the cube is out of view
- **THEN** the row below the picture shows white and green as done and the other four as not yet

#### Scenario: Side at an angle
- **WHEN** the cube is held so that a side is seen at an angle too steep to read
- **THEN** that side's stickers are still marked as known or still needed

### Requirement: Restart with a reason
When the scan cannot make progress, the screen SHALL describe the situation with an icon and a few
words (a description, not an order, e.g. "Heikko valaistus"): too dark for about eight seconds, no
cube found for about thirteen seconds, or no new stickers with the cube in view for about twenty
seconds (including readings that no possible cube fits); each about five seconds later than before
(user, 2026-10-05), so the notice does not come too eagerly. The notice SHALL lie at the bottom of the camera picture without
covering the cube, and the scan SHALL go on underneath it: the user can always keep scanning. The
notice SHALL offer to start the scan again and to go to the colour check with what is known, and,
where the device has a torch, a torch button when the reason is the light or no progress. It
SHALL go away when new stickers become known. A tap anywhere outside it SHALL close it, and it SHALL not come back
for the same reason in that scan. Starting again SHALL clear what was read and keep the camera
running; it is offered only in the notice. Dim light SHALL also be told early, as a small notice on
the picture, before the scan stalls.

#### Scenario: Dim light
- **WHEN** the picture is too dark to read well
- **THEN** a small lamp notice is shown on the picture at once, and if the scan stalls the notice describes the poor light and offers the torch

#### Scenario: No progress
- **WHEN** the cube is in view but nothing new is known for about twenty seconds
- **THEN** the situation is shown at the bottom of the picture with a restart button, and restarting clears the progress

#### Scenario: Keep scanning
- **WHEN** the notice is shown and the user keeps turning the cube
- **THEN** the scan goes on, and the notice goes away as soon as new stickers are known

#### Scenario: Close the notice
- **WHEN** the user taps the picture outside the notice
- **THEN** the notice closes, the scan goes on, and the notice does not come back for the same reason

### Requirement: Reading in different light
The video scan SHALL read colours the same in warm, cool or dim light as in daylight as far as the
picture allows: a sticker's shine SHALL not change its colour, a reading washed out by too much light SHALL count
little, and a reading between two colours SHALL count as uncertain between them rather than as a
sure one, so that the rest of the cube decides. Turning the torch on or off SHALL let the camera
adjust to the new light before readings count again. A cube read in poor light SHALL still never
finish as a wrong cube.

#### Scenario: Warm ceiling light
- **WHEN** the cube is scanned under a dim warm ceiling light in which red looks orange-ish
- **THEN** the scan finishes with the true cube

#### Scenario: Shine on a sticker
- **WHEN** a lamp's reflection lies on part of a sticker
- **THEN** the sticker is read in its own colour

#### Scenario: Torch turned on during the scan
- **WHEN** the user turns the torch on after the scan has started
- **THEN** the picture is not washed out, and orange is not read as yellow nor blue as white

### Requirement: Camera set for the cube
During the video scan the camera SHALL measure the light and focus where the cube is, not on the
whole picture, once a face is found. While the stickers read washed out (too bright to tell their
colours), the camera SHALL be made darker step by step; exposure and white balance SHALL be locked
only once the stickers read well, or when the camera can be made no darker. Turning the torch on or
off SHALL go through the same steps again. The camera's pictures SHALL be read about fifteen times a
second where the device keeps up, and never fewer than before.

#### Scenario: Torch in a dark room
- **WHEN** the user scans in a dark room with the torch on and the cube fills only part of the picture
- **THEN** the stickers are not washed out once the scan has settled, and red and orange are told apart

#### Scenario: Cube moved to another part of the picture
- **WHEN** the cube is first found at the edge of the picture and then held in the middle
- **THEN** the camera stays focused on the cube and the readings stay sharp

#### Scenario: Camera that cannot be made darker
- **WHEN** the camera offers no way to lower its exposure
- **THEN** the scan locks as before and goes on, and washed-out readings count little

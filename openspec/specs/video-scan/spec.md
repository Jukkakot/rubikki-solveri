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

### Requirement: Recognised by agreement
A sticker SHALL count as known only when several frames agree on its colour, or when the rest of
the cube leaves only one possible colour for it. A single wrong frame SHALL NOT change a known
sticker or finish the scan. Red and orange readings SHALL count as weaker evidence against each
other than other colours. Faces SHALL be told apart by how their centres look on this cube in this
light, compared with each other, not only against fixed reference colours: two faces whose centres
look clearly different SHALL never be taken for the same face, even when both are nearer the same
reference colour and are never in view together. The faces seen so far SHALL be named together,
each colour once, and when five have been seen the sixth SHALL follow. When five faces are named
surely, a sixth face seen several times SHALL be named with the colour left, even when its centre
looks more like one of the five; it SHALL NOT wait as unnamed. While a face's centre fits two
colours about equally and the naming cannot tell them apart yet, its stickers SHALL NOT be shown as
known. Two faces found in the same picture SHALL never be taken for the same face: when their
centres name the same colour, the one that fits it better keeps it and the other takes its
next-best colour if it fits nearly as well. Renaming a face SHALL NOT forget what its stickers were
read as: stickers whose readings still agree stay known. Recent readings SHALL count over old ones:
when the readings of a face seen for a while agree with each other and outnumber what was read
before, they SHALL replace known stickers, so that a face read wrong at first is put right by later
clear views.

#### Scenario: One bad frame
- **WHEN** one frame reads a sticker wrong among many that read it right
- **THEN** the sticker keeps its right colour

#### Scenario: Known from the others
- **WHEN** two stickers of a corner are known and its third was never seen
- **THEN** the third is known from the corner's colours

#### Scenario: Dark centre taken for another colour
- **WHEN** the white face and the blue face are in view together and the blue face's centre, in shadow, reads closer to white
- **THEN** the white face is read from the white face only, and the blue face's readings count for the blue face

#### Scenario: Blue face first, white face later
- **WHEN** the scan starts with the blue face on top, its centre reading nearer white, and the white face is shown only later
- **THEN** the two faces' readings are never mixed, and once the white face is seen the blue face is named blue

#### Scenario: Doubtful centre
- **WHEN** a face is seen whose centre fits white and blue about equally, and no other face settles which it is
- **THEN** its stickers are not shown as known until the naming is clear

#### Scenario: Orange face first
- **WHEN** the scan starts with the orange face in view, its centre fitting red and orange about equally, and the red face is shown only later
- **THEN** no sticker of the red side is shown wrong at any time, and once the red face is seen both are named right

#### Scenario: Red centre looks orange
- **WHEN** the white, yellow, green, blue and orange faces are named surely and the red face, whose centre looks more orange than red, is shown several times
- **THEN** the red face is named red, its stickers become known and the scan can finish

#### Scenario: Rename keeps the stickers
- **WHEN** a face whose stickers are known is renamed
- **THEN** the stickers whose readings still agree stay known, and the number of known stickers does not drop

#### Scenario: Face read wrong at first
- **WHEN** a face was known wrong from a long run of bad readings at the start, and the user then shows it to the camera for a few seconds
- **THEN** its stickers change to what the clear views show, and the scan can finish

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
start as the video scan. The video scan screen's menu SHALL offer an entry that switches to the
guided scan, and the guided scan screen's menu an entry that switches back to the video scan;
switching SHALL replace the screen, so going back leads to where the scan was started from.
Rescanning a single face from the colour check SHALL use the guided scan.

#### Scenario: Choosing the way
- **WHEN** the user starts scanning from the home screen
- **THEN** the video scan opens

#### Scenario: Switch to one face at a time
- **WHEN** the user picks "one picture at a time" from the video scan's menu
- **THEN** the guided scan opens in its place, and going back returns to the home screen

#### Scenario: Switch back to video
- **WHEN** the user picks "video" from the guided scan's menu
- **THEN** the video scan opens in its place

#### Scenario: Rescan one face
- **WHEN** the user rescans one face from the colour check
- **THEN** the guided scan opens for that face only

### Requirement: Colours on the camera picture
The camera picture SHALL show both which stickers are still needed and what each known sticker was
read as, without covering the real cube. A sticker still needed SHALL be covered by a grey veil
smaller than the sticker. A known sticker SHALL show a small dot in its read colour at its centre,
small enough that the real sticker shows around it, so a misread can be seen at a glance.

#### Scenario: Reading shown
- **WHEN** a face is found in the picture with some of its stickers still needed
- **THEN** those stickers show a grey veil and the known ones show a small dot in their read colour

#### Scenario: Misread visible
- **WHEN** a sticker is read as a different colour than it really is
- **THEN** its dot shows the read colour, which differs from the real sticker around it

### Requirement: Progress on the real cube
The screen SHALL show the progress on the real cube in the camera picture, not on a separate cube.
Every sticker still needed on a side of the real cube turned towards the camera SHALL be veiled in
grey, so the grey parts show what is left to show; known stickers SHALL show only their small dot.
A side SHALL get a white outline and a small tick at its centre only when the rest of the cube
confirms all its stickers, not on its own readings alone. When the cube's pose cannot be told, at
least the faces found SHALL be marked. A small vibration SHALL tell when new stickers become known.

#### Scenario: Filling in
- **WHEN** a side is shown to the camera and its stickers become known
- **THEN** their grey veils give way to small dots, and the side gets an outline and a tick once the rest of the cube confirms it

#### Scenario: Sure only with the rest of the cube
- **WHEN** a side has been read many times the same way but the rest of the cube does not confirm it yet
- **THEN** its stickers show dots and no veil, but the side has no outline and no tick

#### Scenario: Side at an angle
- **WHEN** the cube is held so that a side is seen at an angle too steep to read
- **THEN** that side's needed stickers are still veiled

#### Scenario: Sides done at a glance
- **WHEN** the white and the green side are confirmed and both are turned towards the camera
- **THEN** both have an outline and a tick, and no row of side colours is shown under the picture

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
sure one, so that the rest of the cube decides. A sticker SHALL be named by its colour more than by
its brightness, so that a colour seen in dimmer or brighter light than its face's centre is still
named right, and a centre that in its light looks like another colour SHALL NOT be the reference for
its own colour. Turning the torch on or off SHALL make the camera adjust to the new light while
reading goes on. A cube read in poor light SHALL still never finish as a wrong cube.

#### Scenario: Warm ceiling light
- **WHEN** the cube is scanned under a dim warm ceiling light in which red looks orange-ish
- **THEN** the scan finishes with the true cube

#### Scenario: Shine on a sticker
- **WHEN** a lamp's reflection lies on part of a sticker
- **THEN** the sticker is read in its own colour

#### Scenario: Torch turned on during the scan
- **WHEN** the user turns the torch on after the scan has started
- **THEN** the picture is not washed out once the camera has adjusted, reading does not pause, and orange is not read as yellow nor blue as white

#### Scenario: Face seen in another light
- **WHEN** the yellow face's centre was seen in bright light, washed out to near white, and the yellow stickers of another face are seen in dimmer light
- **THEN** those stickers are read yellow, not green, and the scan finishes with the true cube

### Requirement: Camera set for the cube
During the video scan the camera SHALL measure the light and focus where the cube is, not on the
whole picture, once a face is found; the point SHALL be the middle of the cube as seen (all faces
found), so it does not jump from face to face as the cube turns. While the stickers read washed out
(too bright to tell their colours), the camera SHALL be made darker step by step; exposure and white
balance SHALL be locked once the stickers read well, or when the camera can be made no darker, and
in any case about a second after the first face was found unless a darkening step is still under
way, however the cube moves meanwhile. No picture SHALL be held back while the camera adjusts:
pictures SHALL be read from the first face on. While a face is found but no sticker has been read
yet, a small spinner SHALL show on the picture. Turning the torch on or off SHALL go through the
same steps again. Every picture the camera delivers SHALL be offered for reading, with no limit of
the app's own; a picture that arrives while the previous one is still being read SHALL be dropped,
so reading never falls behind the camera. A browser that cannot read off the page's thread SHALL
keep about fifteen a second, so the page stays responsive.

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

#### Scenario: Cube turned in the hand from the start
- **WHEN** the user turns the cube in the hand from the moment it is first found, two faces in view, in normal light
- **THEN** the first stickers are read from the first pictures with a face, and the camera locks about a second after the first face was found

#### Scenario: Spinner until the first reading
- **WHEN** a face has been found and no sticker has been read yet
- **THEN** a small spinner shows on the picture, and it goes away as soon as the first sticker is read

### Requirement: Paint follows the cube
The marks SHALL move smoothly with the real cube between readings, without jumps, and SHALL stay on
the cube when a reading misses its pose for a moment. While the cube is held in the hand, with the
small movements that holding brings, the marks SHALL stay. Only while the cube moves clearly fast
(about a side width a second or more) SHALL the marks fade out, and come back once it has slowed for
a moment, so that no mark floats beside a moving cube. When no face has been found for about a
second, the marks SHALL fade out. They SHALL come back as soon as the cube is seen again.

#### Scenario: Turning the cube
- **WHEN** the user turns the cube slowly in front of the camera
- **THEN** the marks glide with the stickers and do not flicker or jump between positions

#### Scenario: Held in the hand
- **WHEN** the user holds the cube in front of the camera with the small movements of a hand
- **THEN** the marks stay shown

#### Scenario: Moving quickly
- **WHEN** the user moves or turns the cube quickly, about a side width a second or faster
- **THEN** the marks fade out while it moves and appear again on the cube soon after it slows

#### Scenario: Pose missed for a moment
- **WHEN** a few pictures in a row give no pose while the cube stays in view
- **THEN** the marks stay on the cube

#### Scenario: Cube out of view
- **WHEN** the cube is taken out of the picture
- **THEN** the marks fade out within about a second, and appear again when the cube is back

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

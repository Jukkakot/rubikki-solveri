## ADDED Requirements

### Requirement: Reading in different light
The video scan SHALL read colours the same in warm, cool or dim light as in daylight as far as the
picture allows: the colour cast of the light SHALL be taken out using the cube's white stickers, a
sticker's shine SHALL not change its colour, a reading washed out by too much light SHALL count
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

## MODIFIED Requirements

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

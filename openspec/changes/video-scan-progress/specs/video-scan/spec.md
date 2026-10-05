## MODIFIED Requirements

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
The solution SHALL then open, with the colour check behind it as for a sure guided scan, stickers
known only from the others marked there. The user SHALL be able to stop earlier and open the check
with what is known. The scan SHALL never stay with everything read and nothing happening.

#### Scenario: Whole cube seen
- **WHEN** the cube that fits the readings is clear for half a second
- **THEN** the solution opens and going back shows the check

#### Scenario: Orange read as red
- **WHEN** one orange sticker has been read as red, so no real piece fits it
- **THEN** the scan takes the piece that fits the rest and finishes

#### Scenario: Stop early
- **WHEN** the user stops the video scan with stickers still unknown
- **THEN** the check opens with the known colours and the unknown stickers marked

### Requirement: Turning hints
While stickers are still needed, a large arrow on the camera picture beside the real cube SHALL
show which way to turn it to bring them into view, with at most a few words. No arrow SHALL be
shown while no face is found or the pose is not known.

#### Scenario: Show the missing side
- **WHEN** only the bottom side still has stickers needed
- **THEN** the arrow asks the user to tilt the cube so the bottom comes into view

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

## ADDED Requirements

### Requirement: Progress on the real cube
The screen SHALL show the progress on the real cube in the camera picture, not on a separate cube.
Every sticker on a side of the real cube turned towards the camera SHALL be marked: a solid mark in
its colour when known, an empty mark when still needed. A side whose stickers are all known SHALL
carry a tick. When the cube's pose cannot be told, at least the faces found SHALL be marked. A small
vibration SHALL tell when new stickers become known. Below the picture a row of the six side
colours SHALL show which sides are fully known, so the progress is visible also while the cube is
out of view.

#### Scenario: Filling in
- **WHEN** a side is shown to the camera and its stickers become known
- **THEN** their empty marks turn into solid marks in their colours, and a tick appears when the side is done

#### Scenario: Sides done at a glance
- **WHEN** the white and the green side are fully known and the cube is out of view
- **THEN** the row below the picture shows white and green as done and the other four as not yet

#### Scenario: Side at an angle
- **WHEN** the cube is held so that a side is seen at an angle too steep to read
- **THEN** that side's stickers are still marked as known or still needed


### Requirement: Restart with a reason
When the scan cannot make progress, the screen SHALL say why with an icon and a few words and
offer to start the scan again: too dark, no cube found for a while, or no new stickers for a while
with the cube in view for about fifteen seconds (including readings that no possible cube fits).
Besides starting again, the user SHALL be able to go to the colour check with what is known.
Starting again SHALL clear what was read and keep the camera running. Dim light SHALL also be told
early, as a small notice on the picture, before the scan stalls.

#### Scenario: Dim light
- **WHEN** the picture is too dark to read well
- **THEN** a small lamp notice is shown on the picture at once, and if the scan stalls the restart panel names the light as the reason

#### Scenario: No progress
- **WHEN** the cube is in view but nothing new is known for about fifteen seconds
- **THEN** the reason and a restart button are shown, and restarting clears the progress

## REMOVED Requirements

### Requirement: Progress cube
**Reason**: The small 3D cube in the corner did not tell the user what was done (phone test 2026-10-05).
**Migration**: Progress is shown on the real cube in the camera picture (Progress on the real cube).

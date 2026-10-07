# manual-input Specification

## Purpose
Lets the user enter their real cube's colours by hand, face by face, and tells them precisely what
is wrong before they try to solve it.

## Requirements

### Requirement: Face-by-face painting
The manual input screen SHALL show one face at a time as a large 3×3 grid, a palette of the six
colours and a hint how to hold the cube so that the face looks at the user (for example: "green
centre towards you, white on top"). Tapping a cell SHALL paint it with the selected colour. The
centre cells SHALL be fixed to the holding position's colours.

#### Scenario: Paint a sticker
- **WHEN** the user selects red and taps the top-left cell of the front face
- **THEN** that sticker becomes red

#### Scenario: Centres are fixed
- **WHEN** the user taps a centre cell
- **THEN** its colour does not change

### Requirement: Face navigation and preview
A map of all six faces SHALL show progress and let the user jump to any face; previous and next
SHALL be ‹ and › icon buttons that go through the faces in the order front, right, back, left,
top, bottom, and the check SHALL be a ✓ main button. A 3D preview SHALL turn to show the face being
edited.

#### Scenario: Next face
- **WHEN** the user is on the front face and taps next
- **THEN** the right face is shown with the hint "red centre towards you, white on top"

#### Scenario: Icons with names for screen readers
- **WHEN** a screen reader reads the bottom bar
- **THEN** it names the buttons "previous", "next" and "check"

### Requirement: Colour counts
The screen SHALL show for each colour how many stickers have it, out of nine, and mark counts
over nine.

#### Scenario: Too many of a colour
- **WHEN** ten stickers are painted white
- **THEN** white shows 10/9 marked as an error

### Requirement: Check
When every sticker has a colour, the user SHALL be able to check the cube. An invalid cube SHALL
give a short message naming the problem in plain words and mark the stickers concerned where
known; a valid cube SHALL be accepted and its solution opened.

#### Scenario: Unfinished cube
- **WHEN** some stickers have no colour
- **THEN** the check is not available

#### Scenario: Invalid cube
- **WHEN** a cube with a twisted corner is checked
- **THEN** a message says one corner is twisted

#### Scenario: Valid cube
- **WHEN** a valid cube is checked
- **THEN** it is accepted and its solution opens

### Requirement: Editing helpers
The screen SHALL offer to clear all stickers and to fill in a solved cube, and SHALL keep the
painting when the screen is rotated.

#### Scenario: Clear
- **WHEN** the user clears the cube
- **THEN** every sticker except the centres has no colour

### Requirement: Check a scan
When opened from a scan, the manual input screen SHALL show the scanned colours as a check that goes
face by face. It SHALL mark the stickers the scan was unsure about or that cause a problem, and say
in one short line what to do (compare with the camera picture, fix a wrong sticker by tapping it,
or scan the face again) until the user's first action on the check. For each face the camera
picture taken during the scan SHALL be shown next to the editable face, when it is still available,
turned the same way as the colours. The screen SHALL offer to scan the whole cube again. Marks on a
face SHALL clear once the user changes that face.

#### Scenario: Opened from a scan
- **WHEN** the scan was unsure about two stickers, both on the right face
- **THEN** the check opens on the right face with those two stickers marked and the instruction

#### Scenario: Instruction only at first
- **WHEN** the user has fixed a sticker or confirmed a face on the check
- **THEN** the instruction line is no longer shown

#### Scenario: Camera picture beside the face
- **WHEN** the check shows the top face after a scan
- **THEN** the top face's camera picture is shown next to the editable top face

#### Scenario: Picture of a turned face
- **WHEN** the top face was scanned turned a quarter
- **THEN** its camera picture in the check is turned back so it matches the colours beside it

#### Scenario: Scan again from the check
- **WHEN** the user chooses to scan the whole cube again on the check
- **THEN** the scan starts again with no face done

### Requirement: One screen
In portrait the manual input screen SHALL fit the display without scrolling: the face map, the
face being edited, the colour palette, previous/next and check SHALL all be visible at once, with
the palette and the buttons at the bottom.

#### Scenario: Everything in view
- **WHEN** the manual input or the check of a scan is open on a phone in portrait
- **THEN** the palette and the check button are visible without scrolling

### Requirement: Face-by-face check
The check of a scan SHALL show which of the six faces are checked. A face with no marked sticker
SHALL start as checked; the check SHALL open on the first unchecked face in the order front, right,
back, left, top, bottom. A "Looks right" action SHALL mark the shown face checked and move to the
next unchecked face. Changing a sticker on a checked face SHALL leave it checked. Any face SHALL be
openable from the face map. While faces remain unchecked, the screen SHALL say how many. In portrait
the face, its picture, the palette, "Looks right" and "Scan this face again" SHALL be visible
without scrolling.

#### Scenario: Only doubtful faces to walk
- **WHEN** a scan had uncertain stickers on the right and top faces only
- **THEN** the other four faces show as checked and the check opens on the right face

#### Scenario: Looks right
- **WHEN** the user taps "Looks right" on the right face and the top face is still unchecked
- **THEN** the right face shows as checked and the top face is shown

#### Scenario: Open any face
- **WHEN** the user taps the front face in the face map during the check
- **THEN** the front face and its picture are shown, still marked as checked

### Requirement: Verdict after the check
When the last face becomes checked, the cube SHALL be checked at once. A solvable cube SHALL open its
solution. A cube that cannot be right SHALL get a plain message that some sticker was read or set
wrong, without cube-theory terms, and name the faces to look at again. Where a swap of two stickers
would make the cube solvable, the faces of the most likely such swap SHALL be named, its two
stickers marked, and those faces set back to unchecked; the check SHALL then open the first of them.
Where no such swap exists, the faces of the stickers the problem concerns SHALL be named, or, when
none are known, the faces with the least sure readings. The likelihood SHALL follow how close each
sticker's camera reading was to the colour it would get.

#### Scenario: Two stickers swapped by the reading
- **WHEN** an orange sticker on the top face and a red sticker on the right face were read the wrong way round and every face is marked "Looks right"
- **THEN** the message says a sticker was read wrong and to look at the top and right faces again, those two stickers are marked, and the check opens on the right face

#### Scenario: Solvable after the check
- **WHEN** the last unchecked face is marked "Looks right" and the colours make a solvable cube
- **THEN** the solution opens

#### Scenario: No single swap helps
- **WHEN** the cube cannot be right, no swap of two stickers makes it solvable and the problem concerns a corner on the front, top and left faces
- **THEN** the message names the front, top and left faces and those faces go back to unchecked

### Requirement: Scan one face again from the check
On the check, "Scan this face again" SHALL open the scan for only the shown face. When that face has
been captured and accepted, the check SHALL return with that face's new colours, worked out from the
colours the rest of this cube was read with, and the face SHALL be unchecked with its doubtful
stickers marked. The other faces, their checked state and the user's fixes on them SHALL be kept.
Leaving the one-face scan without accepting SHALL return to the check unchanged. When the scan's
readings are no longer available (the app was restarted), the action SHALL not be offered.

#### Scenario: Rescan the top face
- **WHEN** the user taps "Scan this face again" on the top face and scans and accepts it
- **THEN** the check shows the top face with the new colours and its new camera picture, and the other faces are as before

#### Scenario: Back from the one-face scan
- **WHEN** the user opens the one-face scan and goes back without accepting
- **THEN** the check is shown as it was

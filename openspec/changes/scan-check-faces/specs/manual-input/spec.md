## MODIFIED Requirements

### Requirement: Check a scan
When opened from a scan, the manual input screen SHALL show the scanned colours as a check that goes
face by face. It SHALL mark the stickers the scan was unsure about or that cause a problem, and say
in one short line what to do: compare with the camera picture, fix a wrong sticker by picking a
colour and tapping it, or scan the face again. For each face the camera picture taken during the
scan SHALL be shown next to the editable face, when it is still available. The screen SHALL offer to
scan the whole cube again. Marks on a face SHALL clear once the user changes that face.

#### Scenario: Opened from a scan
- **WHEN** the scan was unsure about two stickers, both on the right face
- **THEN** the check opens on the right face with those two stickers marked and the instruction

#### Scenario: Camera picture beside the face
- **WHEN** the check shows the top face after a scan
- **THEN** the top face's camera picture is shown next to the editable top face

#### Scenario: Scan again from the check
- **WHEN** the user chooses to scan the whole cube again on the check
- **THEN** the scan starts again from the front face

## ADDED Requirements

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

# Spec Delta

## MODIFIED Requirements

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

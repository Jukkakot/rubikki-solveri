## MODIFIED Requirements

### Requirement: Check a scan
When opened from a scan, the manual input screen SHALL start with the scanned colours on the first
face that has a marked sticker, mark the stickers the scan was unsure about or that cause a
problem, and say in one short line what to do: compare with the camera picture, pick a colour, tap
the wrong sticker. For each face the camera picture taken during the scan SHALL be shown next to the
editable face, when it is still available. The screen SHALL offer to scan again. Marks SHALL clear
once the user changes the cube.

#### Scenario: Opened from a scan
- **WHEN** the scan was unsure about two stickers
- **THEN** the editor shows the scanned cube with those two stickers marked and the instruction

#### Scenario: Camera picture beside the face
- **WHEN** the check opens on the top face after a scan
- **THEN** the top face's camera picture is shown next to the editable top face

#### Scenario: Scan again from the check
- **WHEN** the user taps scan again on the check
- **THEN** the scan starts again from the front face

## ADDED Requirements

### Requirement: One screen
In portrait the manual input screen SHALL fit the display without scrolling: the face map, the
face being edited, the colour palette, previous/next and check SHALL all be visible at once, with
the palette and the buttons at the bottom.

#### Scenario: Everything in view
- **WHEN** the manual input or the check of a scan is open on a phone in portrait
- **THEN** the palette and the check button are visible without scrolling

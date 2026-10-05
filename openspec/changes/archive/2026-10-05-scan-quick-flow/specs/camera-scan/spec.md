# Spec Delta

## MODIFIED Requirements

### Requirement: Result
After the six faces, the scan SHALL name the six centres' colours together, as the one assignment of
the six colours to the six centres that fits their readings best. It SHALL then find how each face
was turned, on the assumption that the real cube is solvable: of all rotations of the six faces, the
one that gives a solvable cube SHALL be used. When none does, the next-best namings of the centres
SHALL be tried in order of fit, and the first that gives a solvable cube SHALL be used. When still
none does, the one with the most stickers forming real pieces SHALL be used, and if the colours of
two opposite centres were read the wrong way round, swapping them SHALL be tried too. When rotations
giving different solvable cubes exist, the faces whose rotation differs SHALL be marked. None of
this SHALL be announced to the user.

A valid scan where no sticker is uncertain or marked SHALL open the solution at once, without the
check; the check with the camera's pictures SHALL stay behind the solution, so going back from the
solution opens it. Otherwise the check SHALL open with the camera's pictures next to the scanned
colours, the uncertain or problem stickers marked and a note asking the user to check them, and
nothing SHALL continue by itself.

#### Scenario: Confident scan
- **WHEN** the scan is valid and confident
- **THEN** the solution opens at once, with no check in between

#### Scenario: Result does not match the pictures
- **WHEN** the check of an unsure scan is open and the user taps scan again
- **THEN** the solution does not open and the scan starts again

#### Scenario: Looking closer
- **WHEN** the solution of a confident scan is open and the user goes back
- **THEN** the check opens with the pictures and the scanned colours, ready to fix

#### Scenario: Faces turned
- **WHEN** a scrambled cube is scanned with the top face turned a quarter and the back face upside down
- **THEN** the solution of the real cube opens

#### Scenario: Unsure scan
- **WHEN** the scan has uncertain stickers or is invalid
- **THEN** the check opens with those stickers marked and does not continue by itself

#### Scenario: Face taken wrong during the scan
- **WHEN** the blue face was taken for the white one during a dim scan (the user's evening scan of 2026-10-04)
- **THEN** the faces are renamed without a message, and the cube is valid

### Requirement: Confirm each face
After a capture the screen SHALL stop reading the camera and show the nine colours as the camera
saw them, with a note that the colours are worked out at the end from the whole cube, and a choice
to go on or scan again. "Good, next" SHALL fill up over a couple of seconds and then accept the
face by itself; a touch on the review SHALL stop that, leaving the choice to the user. During the
full scan it SHALL NOT name the face. Stickers in the review SHALL NOT be tappable. Only an
accepted face SHALL count as done.

#### Scenario: Accept by itself
- **WHEN** a face has been captured and the user does nothing
- **THEN** after a couple of seconds the face counts as done and any face not yet scanned is asked for

#### Scenario: Accept
- **WHEN** a face has been captured and the user taps "Good, next"
- **THEN** the face counts as done at once

#### Scenario: Looking closer
- **WHEN** the user touches the review before the time runs out
- **THEN** it no longer accepts by itself, and "Good, next" or "Scan again" decides

#### Scenario: Centre note in the review
- **WHEN** a face has been captured during the full scan
- **THEN** the review shows its nine colours and the note that the colours are worked out at the end, and names no face

#### Scenario: Scan again
- **WHEN** a face has been captured and the user taps "Scan again"
- **THEN** nothing is stored for it and the camera is read again

#### Scenario: Tap to fix
- **WHEN** the user taps a sticker in the review
- **THEN** no colour changes; doubtful stickers are checked after the scan

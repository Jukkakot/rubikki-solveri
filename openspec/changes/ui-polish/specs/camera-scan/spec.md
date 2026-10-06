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

A valid scan where no sticker is uncertain or marked SHALL open the solution's start screen at once,
without the check; the scan SHALL stay behind it, so going back starts a new scan. The check with
the camera's pictures SHALL be reachable from the guide's menu. Otherwise the check SHALL open with
the camera's pictures next to the scanned colours, the uncertain or problem stickers marked and a
note asking the user to check them, and nothing SHALL continue by itself; going back from it SHALL
start a new scan.

#### Scenario: Confident scan
- **WHEN** the scan is valid and confident
- **THEN** the start screen opens at once, with no check in between

#### Scenario: Result does not match the pictures
- **WHEN** the check of an unsure scan is open and the user taps scan again
- **THEN** the solution does not open and the scan starts again

#### Scenario: Looking closer
- **WHEN** the guide of a confident scan is open and the user picks the colour check from the menu
- **THEN** the check opens with the pictures and the scanned colours, ready to fix

#### Scenario: Back after a confident scan
- **WHEN** the start screen of a confident scan is open and the user goes back
- **THEN** a new scan starts

#### Scenario: Faces turned
- **WHEN** a scrambled cube is scanned with the top face turned a quarter and the back face upside down
- **THEN** the solution of the real cube opens

#### Scenario: Unsure scan
- **WHEN** the scan has uncertain stickers or is invalid
- **THEN** the check opens with those stickers marked and does not continue by itself

#### Scenario: Face taken wrong during the scan
- **WHEN** the blue face was taken for the white one during a dim scan (the user's evening scan of 2026-10-04)
- **THEN** the faces are renamed without a message, and the cube is valid

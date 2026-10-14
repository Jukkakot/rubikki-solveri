## MODIFIED Requirements

### Requirement: Start screen
Before the guide of a scanned, entered, free or pattern cube, a start screen SHALL show:
- the number of moves;
- the target with a way to change it;
- the method;
- "Aloita".

"Aloita" SHALL open the hold step. The start screen SHALL NOT show the holding position or the automatic
mode. Practice and the timer's scramble SHALL open the guide directly.

#### Scenario: After a scan
- **WHEN** a scan finishes with a valid cube
- **THEN** the start screen shows the number of moves, the solved target and the fastest method, and no automatic-mode button

#### Scenario: Start
- **WHEN** the user taps "Aloita"
- **THEN** the hold step opens

#### Scenario: Practice skips it
- **WHEN** the user starts a practice position from a lesson
- **THEN** the guide opens directly

### Requirement: Guide menu
The guide SHALL have a ⋮ menu with camera follow, the colour check of the cube being solved, a way back to
the start screen and, where the automatic mode is offered, its speed (slow, normal, fast; the current one
marked). The guide SHALL NOT show the method or target choice itself. The holding position SHALL be stated
in words only on the first move and when the hold changes.

#### Scenario: Camera follow from the menu
- **WHEN** the user picks camera follow from the menu
- **THEN** camera mode starts on the current move

#### Scenario: Check the colours
- **WHEN** the user picks the colour check from the menu
- **THEN** the check opens with the colours of the cube being solved

#### Scenario: Speed from the menu
- **WHEN** the user picks "fast" in the menu
- **THEN** the automatic mode runs at the fast speed when started, and "fast" is marked the next time the menu opens

#### Scenario: Hold text only when needed
- **WHEN** the guide is on move 5 and the hold has not changed since move 1
- **THEN** no holding text is shown

### Requirement: Back from the solution
Going back from the guide SHALL open the start screen. Going back from the hold step SHALL open the start
screen. Going back from the start screen of a scanned cube SHALL open a new scan; from any other start
screen, the screen it came from.

#### Scenario: Back twice after a scan
- **WHEN** the user goes back from the guide of a scanned cube and then back again
- **THEN** the start screen opens and then a new video scan

#### Scenario: Back from the hold step
- **WHEN** the user goes back from the hold step
- **THEN** the start screen opens

## ADDED Requirements

### Requirement: Hold step
After "Aloita" and before the guide, a hold step SHALL show "Pidä kuutiota näin": a big cube in the holding
position held by two drawn hands, the hold in words, and one primary button "Olen valmis". Only "Olen valmis"
SHALL open the guide, at the first move in the manual mode.

#### Scenario: Picture of the hold
- **WHEN** the hold step is open
- **THEN** the cube is seen as in the guide, it does not turn when dragged, and two hands hold it from the left and right with the thumbs on the front, readable on a light and a dark background

#### Scenario: Ready opens the guide
- **WHEN** the hold step is open and the user taps "Olen valmis"
- **THEN** the guide opens at move 1 and does not advance by itself

#### Scenario: Hold in the picture and in words
- **WHEN** the hold step opens for a cube with the white centre towards the user and blue on top
- **THEN** the big cube shows white at the front and blue on top, held by two hands, and the words say white towards you, blue on top

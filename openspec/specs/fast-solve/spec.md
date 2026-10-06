# fast-solve Specification

## Purpose
Gives the user a short solution for their cube and guides them through it one move at a time.

## Requirements

### Requirement: Short solution
For any valid cube the app SHALL find a solution of at most 21 face turns, about 19 on average,
in under a second on the reference phone, without blocking the screen. An already solved cube
SHALL be reported as solved; an invalid cube SHALL get the same message as the manual check.

#### Scenario: Scrambled cube
- **WHEN** a valid scrambled cube is solved
- **THEN** the moves, applied to it, solve it and there are at most 21 of them

#### Scenario: Solved cube
- **WHEN** the cube is already solved
- **THEN** the screen says it is already solved

### Requirement: Step through the solution
The guide SHALL show the cube of the current move (before it until its demo plays, after it once
the demo has played), the move in words, and a timeline with the move number, the total and a bar.
Its controls SHALL be those of a media player: ⏮ SHALL animate the undo of the previous move and
then demo it; ⏭ ("Done") SHALL go to the next move without replaying the turn; ▶/⏸ SHALL start and
stop handsfree where handsfree is offered. A ↻ icon on the cube SHALL play the current move from
before it. After the last move the screen SHALL say the cube is solved and celebrate it: the cube
hops and spins once around (about a second) while confetti in the six sticker colours bursts from
it, with a success vibration. With animations off the solved cube SHALL be shown without motion.

#### Scenario: Next and back
- **WHEN** the user taps ⏭ on move 1 of 19 and then ⏮
- **THEN** move 2 of 19 is shown, then the undo of move 1 animates and move 1 of 19 is shown again and demoed

#### Scenario: Show again
- **WHEN** the user taps ↻ on the cube
- **THEN** the current move plays from the state before it

#### Scenario: Finished
- **WHEN** the user taps ⏭ on the last move
- **THEN** the screen says the cube is solved, the cube hops and spins once with a burst of confetti, and the phone vibrates

### Requirement: Moves in words
Each move SHALL be described in the app's language, the same way everywhere in the app (both
solution methods, camera follow, lessons): as the user sees it in the holding view, the top and
bottom layers turn to the left or right, the right and left sides turn up or down, the back side
by which way its top row moves, and the front clockwise or counter-clockwise; half turns say half
a turn; a whole-cube turn names the centre that comes towards the user and the one on top. Other
texts that name a layer or a direction (step notes, lesson texts, tips) SHALL use the same terms.
The holding position SHALL be stated on the screen.

#### Scenario: Top move in the fast method
- **WHEN** the current move is a counter-clockwise turn of the top (U') in the fast method
- **THEN** it reads "Turn the top layer to the right"

#### Scenario: Back move in the fast method
- **WHEN** the current move is a clockwise turn of the back (B) in the fast method
- **THEN** it reads "Turn the back side so its top row moves to the left"

#### Scenario: Front move in the fast method
- **WHEN** the current move is a clockwise turn of the front (F) in the fast method
- **THEN** it reads "Turn the front side clockwise"

#### Scenario: Describe a move
- **WHEN** the current move is a counter-clockwise turn of the top in the learn method or in a lesson's algorithm demo
- **THEN** it reads "Turn the top layer to the right", the same as in the fast method

### Requirement: Start screen
Before the guide of a scanned, entered, free or pattern cube, a start screen SHALL show:
- the number of moves;
- the target with a way to change it;
- the method;
- the holding position as a small picture of the cube;
- "Aloita" and, where handsfree is offered, "Handsfree" with its speed.

"Aloita" SHALL open the guide at the first move. Practice and the timer's scramble SHALL open the
guide directly.

#### Scenario: After a scan
- **WHEN** a scan finishes with a valid cube
- **THEN** the start screen shows the number of moves, the solved target, the fastest method and the holding picture

#### Scenario: Start
- **WHEN** the user taps "Aloita"
- **THEN** the guide opens at move 1

#### Scenario: Practice skips it
- **WHEN** the user starts a practice position from a lesson
- **THEN** the guide opens directly

### Requirement: Guide menu
The guide SHALL have a ⋮ menu with camera follow, the colour check of the cube being solved, and a
way back to the start screen. The guide SHALL NOT show the method or target choice itself. The
holding position SHALL be stated in words only on the first move and when the hold changes.

#### Scenario: Camera follow from the menu
- **WHEN** the user picks camera follow from the menu
- **THEN** camera mode starts on the current move

#### Scenario: Check the colours
- **WHEN** the user picks the colour check from the menu
- **THEN** the check opens with the colours of the cube being solved

#### Scenario: Hold text only when needed
- **WHEN** the guide is on move 5 and the hold has not changed since move 1
- **THEN** no holding text is shown

### Requirement: Back from the solution
Going back from the guide SHALL open the start screen. Going back from the start screen of a scanned
cube SHALL open a new scan; from any other start screen, the screen it came from.

#### Scenario: Back twice after a scan
- **WHEN** the user goes back from the guide of a scanned cube and then back again
- **THEN** the start screen opens and then a new video scan

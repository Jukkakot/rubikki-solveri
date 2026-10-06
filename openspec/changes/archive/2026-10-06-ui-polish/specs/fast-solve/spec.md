# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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

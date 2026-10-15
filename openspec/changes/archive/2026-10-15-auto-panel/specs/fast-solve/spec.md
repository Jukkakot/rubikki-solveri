## MODIFIED Requirements

### Requirement: Step through the solution
The guide SHALL show the cube of the current move (before it until its demo plays, after it once
the demo has played), the move in words, and a timeline with the move number, the total and a bar.
Its controls SHALL be a small ⏮ and one wide primary button "Seuraava ⏭": ⏮ SHALL animate the undo
of the previous move and then demo it; "Seuraava" SHALL go to the next move without replaying the
turn. While the automatic mode runs, the wide button SHALL read "Pysäytä". A ↻ icon beside the move
words SHALL play the current move from before it. After the last move the screen SHALL say the cube
is solved and celebrate it: the cube hops and spins once around (about a second) while confetti in
the six sticker colours bursts from it, with a success vibration. With animations off the solved cube
SHALL be shown without motion.

#### Scenario: Next and back
- **WHEN** the user taps "Seuraava" on move 1 of 19 and then ⏮
- **THEN** move 2 of 19 is shown, then the undo of move 1 animates and move 1 of 19 is shown again and demoed

#### Scenario: Show again
- **WHEN** the user taps ↻
- **THEN** the current move plays from the state before it

#### Scenario: Finished
- **WHEN** the user taps "Seuraava" on the last move
- **THEN** the screen says the cube is solved, the cube hops and spins once with a burst of confetti, and the phone vibrates

### Requirement: Guide menu
The guide SHALL have a ⋮ menu with camera follow, the colour check of the cube being solved and a way back
to the start screen. The guide SHALL NOT show the method or target choice itself, and the menu SHALL NOT
hold the automatic mode or its speed. The holding position SHALL be stated in words only on the first move
and when the hold changes.

#### Scenario: Camera follow from the menu
- **WHEN** the user picks camera follow from the menu
- **THEN** camera mode starts on the current move

#### Scenario: Check the colours
- **WHEN** the user picks the colour check from the menu
- **THEN** the check opens with the colours of the cube being solved

#### Scenario: Speed from the menu
- **WHEN** the user opens the menu
- **THEN** it has no speed choice: the speed is chosen in the automatic mode's panel

#### Scenario: Hold text only when needed
- **WHEN** the guide is on move 5 and the hold has not changed since move 1
- **THEN** no holding text is shown

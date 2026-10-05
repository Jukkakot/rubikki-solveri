# Spec Delta

## MODIFIED Requirements

### Requirement: Step through the solution
The solution screen SHALL show the cube of the current move (before it until its demo plays,
after it once the demo has played), the move number out of the total with a progress bar, and the
move in words. "Done" SHALL go to the next move without replaying the turn; "back" SHALL animate
the undo of the previous move and then demo it; "show" SHALL play the current move from before it.
After the last move the screen SHALL say the cube is solved and celebrate it: the cube hops and
spins once around (about a second) while confetti in the six sticker colours bursts from it, with
a success vibration. With animations off the solved cube SHALL be shown without motion.

#### Scenario: Next and back
- **WHEN** the user taps done on move 1 of 19 and then back
- **THEN** move 2 of 19 is shown, then the undo of move 1 animates and move 1 of 19 is shown again and demoed

#### Scenario: Finished
- **WHEN** the user taps done on the last move
- **THEN** the screen says the cube is solved, the cube hops and spins once with a burst of confetti, and the phone vibrates

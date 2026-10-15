# handsfree-guide Specification

## Purpose
Lets the user follow the shortest solution without taking a hand off the cube to aim at a button:
a tap on the cube confirms a move, and a handsfree mode advances by itself at a chosen speed.

## Requirements

### Requirement: Tap the cube to confirm
In the shortest-solution guide (also when solving to a target), a tap anywhere on the 3D cube
SHALL count as "Tein sen", with the same vibration and step change. A drag SHALL keep turning the
view and SHALL NOT confirm. Back and "Näytä" SHALL stay buttons. Until the first move of a solve
has been confirmed, a hint on the cube SHALL tell that a tap confirms and a drag turns the view.
The learn method, practice and the timer's scramble SHALL keep the button only.

#### Scenario: Tap confirms
- **WHEN** the user taps the cube while a move is presented
- **THEN** the phone vibrates and the next move appears, as with "Tein sen"

#### Scenario: Drag does not confirm
- **WHEN** the user drags the cube
- **THEN** the view turns and the move stays the same

#### Scenario: Hint until the first move
- **WHEN** a solve starts in the shortest-solution guide
- **THEN** the hint is shown on the cube, and it is gone once the first move is confirmed

### Requirement: Start handsfree
Handsfree (shown to the user as "Automaattinen", English "Autoplay") SHALL be offered in the
shortest-solution guide (also to a target), not in camera follow, and not once the solve is finished. The
guide SHALL always open in the manual mode. A small "Auto" button beside the move words SHALL open a panel
from the bottom with what the mode does, the speed as a slider of five steps shown in seconds per move, and
"Käynnistä"; only "Käynnistä" SHALL start handsfree, at once and from the current move. Closing the panel
otherwise SHALL change nothing but the speed. The chosen speed SHALL be remembered between solves;
handsfree itself SHALL NOT be.

#### Scenario: Manual by default
- **WHEN** the user taps "Olen valmis" in the hold step
- **THEN** the guide opens at move 1 and waits for the user; nothing advances by itself

#### Scenario: Ready prompt waits
- **WHEN** the user taps "Auto"
- **THEN** the panel opens and nothing advances until the user taps "Käynnistä"

#### Scenario: Start from the guide
- **WHEN** the user taps "Käynnistä" in the panel
- **THEN** the panel closes and handsfree runs at once from the current move at the speed set in the panel

#### Scenario: Speed remembered
- **WHEN** the user sets the slider to 1,5 s per move and later opens the panel in another solve
- **THEN** the slider is at 1,5 s per move

### Requirement: Advance by itself
While handsfree runs, each move SHALL be presented as in the normal guide (arrow, demo), and the
guide SHALL move on as if "Tein sen" had been pressed once the move's time has passed since the move
appeared, without a replay of the turn and without waiting for the demo to end. The time SHALL
depend on the speed and be longer for a half turn than for a quarter turn. A bar SHALL fill over the
time, and a light vibration SHALL come shortly before the guide moves on. The demo SHALL NOT repeat
while handsfree runs. The screen SHALL stay on while handsfree runs. When the cube is solved,
handsfree SHALL end with the normal finish.

#### Scenario: Moves on after the time
- **WHEN** handsfree runs at normal speed and a quarter turn appears
- **THEN** the bar starts filling at once while the demo plays, a light vibration comes just before the end, and the next move appears when the time is up

#### Scenario: Half turn gets more time
- **WHEN** a half turn is presented in handsfree
- **THEN** the guide waits longer before moving on than for a quarter turn at the same speed

#### Scenario: Finish
- **WHEN** the last move's time has run out
- **THEN** the solved finish is shown and handsfree has ended

### Requirement: Any touch stops handsfree
While handsfree runs, a band above the move words SHALL say that it runs and at what speed, and a touch
anywhere on the screen SHALL stop it and return to the normal guide on the same move; that touch SHALL do
nothing else (it does not confirm, go back or turn the view). The normal guide's demo repeat SHALL resume,
the band SHALL go and the wide button SHALL read "Seuraava" again. Leaving the screen SHALL end handsfree.
Handsfree SHALL start again through "Auto".

#### Scenario: Touch stops on the same move
- **WHEN** handsfree runs on move 5 and the user touches the screen
- **THEN** the guide is in the normal mode on move 5, the button reads "Seuraava", and it does not advance by itself

#### Scenario: Restart
- **WHEN** the user has stopped handsfree, taps "Auto" and then "Käynnistä"
- **THEN** handsfree continues from the current move at once

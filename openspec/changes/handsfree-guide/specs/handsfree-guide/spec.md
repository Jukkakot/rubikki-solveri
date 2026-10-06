# Spec Delta

## Purpose
Lets the user follow the shortest solution without taking a hand off the cube to aim at a button:
a tap on the cube confirms a move, and a handsfree mode advances by itself at a chosen speed.

## ADDED Requirements

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
The shortest-solution guide SHALL have a "Handsfree" button while the 3D guide is shown and the
solve is not finished. It SHALL open a ready prompt that tells the user to take the cube in hand,
offers the speed (slow, normal, fast) and a big "Valmis" button; nothing advances until "Valmis"
is pressed. The chosen speed SHALL be remembered between solves; handsfree itself SHALL NOT be
(each solve starts in the normal guide). The button SHALL NOT be shown in camera follow.

#### Scenario: Ready prompt waits
- **WHEN** the user taps "Handsfree"
- **THEN** the ready prompt is shown and the guide does not advance until "Valmis" is pressed

#### Scenario: Speed remembered
- **WHEN** the user picks "fast" and later starts handsfree in another solve
- **THEN** the prompt has "fast" selected

### Requirement: Advance by itself
While handsfree runs, each move SHALL be presented as in the normal guide (arrow, demo) and, once
its demo has ended, the guide SHALL wait the move's time and then move on as if "Tein sen" had been
pressed, without a replay of the turn. The time SHALL depend on the speed and be longer for a half
turn than for a quarter turn. A bar SHALL fill over the time left, and a light vibration SHALL come
shortly before the guide moves on. The demo SHALL NOT repeat while handsfree runs. With animations
off the time SHALL start as soon as the move appears. The screen SHALL stay on while handsfree runs.
When the cube is solved, handsfree SHALL end with the normal finish.

#### Scenario: Moves on after the time
- **WHEN** handsfree runs at normal speed and the demo of a quarter turn ends
- **THEN** the bar fills, a light vibration comes just before the end, and the next move appears

#### Scenario: Half turn gets more time
- **WHEN** a half turn is presented in handsfree
- **THEN** the guide waits longer before moving on than for a quarter turn at the same speed

#### Scenario: Finish
- **WHEN** the last move's time has run out
- **THEN** the solved finish is shown and handsfree has ended

### Requirement: Any touch stops handsfree
While handsfree runs, a touch anywhere on the screen SHALL stop it and return to the normal guide
on the same move; that touch SHALL do nothing else (it does not confirm, go back or turn the view).
The normal guide's demo repeat SHALL resume. Leaving the screen SHALL end handsfree. Handsfree SHALL
start again only through the "Handsfree" button and "Valmis".

#### Scenario: Touch stops on the same move
- **WHEN** handsfree runs on move 5 and the user touches the screen
- **THEN** the guide is in the normal mode on move 5 and does not advance by itself

#### Scenario: Restart
- **WHEN** the user has stopped handsfree and taps "Handsfree" again
- **THEN** the ready prompt is shown and handsfree continues from the current move after "Valmis"

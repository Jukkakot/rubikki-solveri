# Spec Delta

## MODIFIED Requirements

### Requirement: Start handsfree
Handsfree SHALL be offered in the shortest-solution guide (also to a target), not in camera follow,
and not once the solve is finished. It SHALL start, with no ready prompt, from "Handsfree" on the
start screen or from ▶ in the guide. The start screen SHALL offer the speed (slow, normal, fast).
The chosen speed SHALL be remembered between solves; handsfree itself SHALL NOT be.

#### Scenario: Ready prompt waits
- **WHEN** the user taps "Handsfree" on the start screen
- **THEN** no ready prompt is shown: the guide opens at move 1 and handsfree runs at the chosen speed

#### Scenario: Start from the guide
- **WHEN** the user taps ▶ in the guide
- **THEN** handsfree runs at once from the current move at the remembered speed

#### Scenario: Speed remembered
- **WHEN** the user picks "fast" and later opens the start screen of another solve
- **THEN** "fast" is selected

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
While handsfree runs, a touch anywhere on the screen SHALL stop it and return to the normal guide
on the same move; that touch SHALL do nothing else (it does not confirm, go back or turn the view).
The normal guide's demo repeat SHALL resume, and ⏸ SHALL turn back into ▶. Leaving the screen SHALL
end handsfree. Handsfree SHALL start again through ▶.

#### Scenario: Touch stops on the same move
- **WHEN** handsfree runs on move 5 and the user touches the screen
- **THEN** the guide is in the normal mode on move 5, shows ▶, and does not advance by itself

#### Scenario: Restart
- **WHEN** the user has stopped handsfree and taps ▶
- **THEN** handsfree continues from the current move at once

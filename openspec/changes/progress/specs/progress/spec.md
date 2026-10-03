# Spec Delta

## Purpose

Lets the user time their solves, see their history and statistics, and see how much they have
practised — all stored on the phone.

## ADDED Requirements

### Requirement: Scramble
The timer screen SHALL show a random scramble that leads to a uniformly random cube, as notation,
and SHALL offer to perform it with the move guide. A new scramble SHALL be shown after each solve.

#### Scenario: New scramble
- **WHEN** a solve is saved
- **THEN** a different scramble is shown

### Requirement: Timer
Holding a finger on the timer for half a second SHALL make it ready (shown in green); releasing
SHALL start it; a tap SHALL stop it. A release before it is ready SHALL not start it. The time
SHALL be shown in seconds with hundredths.

#### Scenario: Hold, release, stop
- **WHEN** the user holds for 0.6 s, releases, and taps 12.34 s later
- **THEN** the time 12.34 is shown and saved

#### Scenario: Released too early
- **WHEN** the user releases after 0.2 s
- **THEN** the timer does not start

### Requirement: Penalties
The last solve SHALL be markable +2 (two seconds added) or DNF (did not finish), and any solve in
the history SHALL be markable or deletable.

#### Scenario: Plus two
- **WHEN** the last solve of 12.34 is marked +2
- **THEN** it counts as 14.34

### Requirement: Statistics
The timer SHALL show the best time, the average of the last 5 and of the last 12 (dropping the best
and worst of those; one DNF counts as the worst, two make the average DNF), the mean and the count.

#### Scenario: Average of five
- **WHEN** the last five are 10, 12, 11, 30 and 9 seconds
- **THEN** the average of 5 is 11.00

#### Scenario: Two DNFs
- **WHEN** two of the last five are DNF
- **THEN** the average of 5 is DNF

### Requirement: History and recording
The history SHALL list timed solves newest first, guided solutions (method, number of moves, time
taken) and practice sessions per stage. Finishing a guided solution or a practice SHALL record it.
Data SHALL stay on the phone.

#### Scenario: Guided solve recorded
- **WHEN** the user finishes a step-by-step solution
- **THEN** it appears in the history with its method and number of moves

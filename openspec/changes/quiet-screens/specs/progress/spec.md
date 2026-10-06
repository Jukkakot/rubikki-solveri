# Spec Delta

## MODIFIED Requirements

### Requirement: Timer
Holding a finger on the timer for half a second SHALL make it ready (shown in green); releasing
SHALL start it; a tap SHALL stop it. A release before it is ready SHALL not start it. The time
SHALL be shown in seconds with hundredths. How to start the timer SHALL be explained in the timer
area only until the first timed solve has been saved.

#### Scenario: Hold, release, stop
- **WHEN** the user holds for 0.6 s, releases, and taps 12.34 s later
- **THEN** the time 12.34 is shown and saved

#### Scenario: Released too early
- **WHEN** the user releases after 0.2 s
- **THEN** the timer does not start

#### Scenario: Instruction only at first
- **WHEN** the user has timed solves and opens the timer
- **THEN** the timer area shows the time alone, without the hold-and-release instruction

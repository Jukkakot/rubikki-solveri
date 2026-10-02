# Spec Delta

## Purpose

Lets the user (and Claude, from a shared log) find out what the app did and why something failed,
without any server: everything is kept on the phone.

## ADDED Requirements

### Requirement: Local log
The app SHALL record its key events and errors as one line each, with time, level, an event name
from a fixed catalogue and the event's fields. Lines SHALL go to the system log and to a file on
the phone whose size is capped by dropping the oldest lines. Nothing SHALL be sent off the phone.

#### Scenario: Event recorded
- **WHEN** the app starts
- **THEN** a line with the start event and the app version is in the log file

#### Scenario: Log stays small
- **WHEN** the log file grows past its cap
- **THEN** the oldest lines are dropped and the newest kept

### Requirement: Crash capture
An uncaught error SHALL be written to the log with its stack trace on a single line before the app
closes. On the next start the app SHALL tell the user briefly that it crashed last time and offer
the log; technical details SHALL appear only in the log, never in normal UI text.

#### Scenario: Crash then restart
- **WHEN** the app crashes and is opened again
- **THEN** the crash is in the log and a short notice offers to open the log

### Requirement: Log viewer
Settings SHALL lead to a log screen that shows the newest lines first and lets the user share the
log file through the phone's share sheet or clear it.

#### Scenario: Share the log
- **WHEN** the user opens the log screen and taps share
- **THEN** the phone's share sheet opens with the log file

#### Scenario: Clear the log
- **WHEN** the user taps clear on the log screen
- **THEN** the log is emptied and the screen shows no lines

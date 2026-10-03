# Proposal

## Why

Learning shows in numbers: getting faster, solving without help, practising every stage. The user
wants a timer, a history of solves and simple statistics, kept on the phone.

## What Changes

- Timer screen: a random-state scramble (notation, and a guided "scramble with the guide" using the
  move guide), a big timer started by holding and releasing the screen and stopped by a tap, with
  +2 and DNF for the last solve.
- Statistics: best, average of 5 and of 12 (best and worst dropped, as in competitions), mean and
  count.
- History: all timed solves (time, date, scramble) with +2 / DNF / delete, plus guided solves
  (method, moves, time taken) and practice sessions per stage.
- Guided solutions and stage practice are recorded when finished; the lessons list shows how many
  times each stage has been practised.
- All data in a local database on the phone (Room); nothing leaves the phone.

## Capabilities

### New Capabilities
- `progress`: timing solves, keeping their history and showing statistics.

### Modified Capabilities
- `lessons`: the lesson list shows practice counts.
- `app-shell`: the home screen gains the timer entry.

## Impact

- `app`: Room + KSP, `ProgressDatabase`, `ProgressRepository`, `TimerScreen`, `HistoryScreen`,
  recording hooks in the solution screen; pure Kotlin stats and timer state with JVM tests.

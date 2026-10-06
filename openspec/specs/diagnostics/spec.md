# diagnostics Specification

## Purpose
Lets the user (and Claude, from a shared log) find out what the app did and why something failed,
without any server: everything is kept on the phone.

## Requirements

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
log file through the phone's share sheet or clear it. Each line SHALL show its time in the phone's
time zone, formatted the way the app's language writes dates and times; a line from today SHALL
show only the time. Lines SHALL be coloured by level: errors in the error colour, warnings in a
warning colour, debug lines muted, info lines in the normal text colour. The shared log file SHALL
keep its own unchanged format.

#### Scenario: Share the log
- **WHEN** the user opens the log screen and taps share
- **THEN** the phone's share sheet opens with the log file

#### Scenario: Clear the log
- **WHEN** the user taps clear on the log screen
- **THEN** the log is emptied and the screen shows no lines

#### Scenario: Local time
- **WHEN** the phone is in Finland (UTC+3 in summer), the app is in Finnish and a line was written today at 08:09:51 UTC
- **THEN** the line shows 11.09.51

#### Scenario: An earlier day
- **WHEN** a line was written on an earlier day
- **THEN** it shows the date and the time in the language's format

#### Scenario: Errors stand out
- **WHEN** the log has an error line among info lines
- **THEN** the error line is drawn in the error colour and the info lines in the normal colour

### Requirement: Scan pictures
Each capture in the scan SHALL save a small picture of the grid area on the phone, and the log line
of that capture SHALL name the picture. Only the newest twelve pictures SHALL be kept. Sharing the
log SHALL send the pictures together with the log file, and clearing the log SHALL delete them.
Pictures SHALL never leave the phone except when the user shares the log.

#### Scenario: Picture of a capture
- **WHEN** a face is captured
- **THEN** a picture of the grid area is saved and the capture's log line names it

#### Scenario: Share with pictures
- **WHEN** the user shares the log after a scan
- **THEN** the share sheet gets the log file and the scan pictures

#### Scenario: Clear removes pictures
- **WHEN** the user clears the log
- **THEN** the scan pictures are deleted too

### Requirement: Video scan log
While the video scan runs, the log SHALL get a snapshot every couple of seconds of what the scan
knows (per side, the best cube so far with its unsure stickers, how sure it is, brightness, faces
per frame, time per frame, frames read per second, whether the torch is on, how much darker the
camera has been set) and a line for each turning point: a side done, the cube clear, a stall and its
reason, a restart, leaving the screen, the camera's exposure locked. When the camera opens, one line
SHALL tell which camera it is and what it can do (its resolution, focus modes, how much darker or
brighter it can be set, whether it can measure light at a point, torch), on the phone and in the
browser. A minute of scanning SHALL stay at a few dozen lines.

#### Scenario: Stuck scan readable from the log
- **WHEN** a video scan stalls and the user shares the log
- **THEN** the log shows the last snapshots with the cube read so far and the stall's reason

#### Scenario: Camera's abilities in the log
- **WHEN** the user opens the video scan in the phone's browser and shares the log
- **THEN** the log names the camera and says whether it can focus and measure light at a point and how far its exposure can be lowered

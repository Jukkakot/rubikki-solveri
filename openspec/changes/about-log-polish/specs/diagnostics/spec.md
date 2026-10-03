## MODIFIED Requirements

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

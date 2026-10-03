## REMOVED Requirements

### Requirement: Home screen
**Reason**: Replaced by "Home screen layout"; every feature is built, so the "coming later" rule for unbuilt entries no longer applies.
**Migration**: See "Home screen layout" below.

## ADDED Requirements

### Requirement: Home screen layout
The app SHALL open on a home screen that shows the app name and the 3D cube, which turns slowly on
its own and can be turned by dragging. Scanning the cube SHALL be the one primary action, shown
larger and more prominent than the rest. The other main features (enter colours by hand, learn,
timer and statistics, free cube) SHALL be shown as equal secondary entries, each with an icon and
a label. The whole screen SHALL fit a phone screen in portrait and in landscape without scrolling.

#### Scenario: App starts on home
- **WHEN** the user opens the app
- **THEN** the home screen with the app name and the turning cube is shown

#### Scenario: Scan is the main action
- **WHEN** the home screen is shown
- **THEN** "scan the cube" is the single most prominent action and the other features are shown as smaller entries of equal weight

#### Scenario: Turn the cube
- **WHEN** the user drags the cube on the home screen
- **THEN** the cube turns with the finger, and it resumes turning on its own after the user lets go

#### Scenario: Open manual input
- **WHEN** the user taps "enter colours by hand"
- **THEN** the manual input screen opens on the front face

#### Scenario: Open the scan
- **WHEN** the user taps "scan the cube"
- **THEN** the scan screen opens

#### Scenario: Open lessons
- **WHEN** the user taps "learn"
- **THEN** the lessons list opens

#### Scenario: Open the timer
- **WHEN** the user taps "timer and statistics"
- **THEN** the timer opens with a scramble

#### Scenario: Open the free cube
- **WHEN** the user taps "free cube"
- **THEN** the free cube screen opens

### Requirement: Solve summary on the home screen
When the user has timed solves, the home screen SHALL show one short line with the best time and
the number of timed solves. When there are none, the line SHALL NOT be shown.

#### Scenario: With timed solves
- **WHEN** the user has timed solves and opens the home screen
- **THEN** a line shows the best time and how many timed solves there are

#### Scenario: No timed solves
- **WHEN** the user has no timed solves
- **THEN** no summary line is shown

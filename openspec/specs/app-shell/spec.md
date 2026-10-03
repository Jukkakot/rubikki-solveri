# app-shell Specification

## Purpose
The frame of the app: the home screen that leads to every feature, the settings, and the look and
language the whole app follows.

## Requirements

### Requirement: Settings screen
The home screen SHALL lead to a settings screen, and the system back action SHALL return from it
to the home screen.

#### Scenario: Open and leave settings
- **WHEN** the user taps settings on the home screen and then goes back
- **THEN** the settings screen is shown and back returns to the home screen

### Requirement: Language
All UI text SHALL be available in Finnish and English. Finnish SHALL be used until the user picks
another language in settings; the choice SHALL apply at once and persist across restarts.

#### Scenario: Default language
- **WHEN** the app is started for the first time
- **THEN** its texts are in Finnish

#### Scenario: Switch to English
- **WHEN** the user picks English in settings
- **THEN** the texts change to English at once and stay English after a restart

### Requirement: Theme
The app SHALL use the phone's dynamic (Material You) colours, with designed light and dark
variants. It SHALL follow the phone's dark mode unless the user forces light or dark in settings;
the choice SHALL persist across restarts.

#### Scenario: Follow the phone
- **WHEN** the theme setting is "follow the phone" and the phone is in dark mode
- **THEN** the app is dark

#### Scenario: Forced light
- **WHEN** the user picks light in settings while the phone is in dark mode
- **THEN** the app is light and stays light after a restart

### Requirement: Version on the home screen
The home screen SHALL show, small and below the entries, the app version and the date and time this
build was installed or last updated, so the user can tell that an update arrived. If the install
time cannot be read, the version alone SHALL be shown.

#### Scenario: After an update
- **WHEN** the user installs a new build and opens the app
- **THEN** the home screen shows the new version and the install time of that build

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

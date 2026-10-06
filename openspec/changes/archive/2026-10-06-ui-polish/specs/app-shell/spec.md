# Spec Delta

## MODIFIED Requirements

### Requirement: Home screen layout
The app SHALL open on a home screen that shows the app name and the 3D cube, which turns slowly on
its own and can be turned by dragging. The cube SHALL be the scan action: a tap on it, or on the round
camera button on its lower edge, SHALL open the scan; a drag SHALL only turn it. The other main
features (enter colours by hand, learn, timer and statistics, free cube, patterns) SHALL be shown
below as a row of equal icons, each with a one-word label; there SHALL be no separate entry for a
second way of scanning and no tagline. The whole screen SHALL fit a phone screen in portrait and in
landscape without scrolling, also in the phone's browser, where the cube shrinks to the space left
and never covers the texts.

#### Scenario: App starts on home
- **WHEN** the user opens the app
- **THEN** the home screen with the app name and the turning cube is shown

#### Scenario: Scan is the main action
- **WHEN** the home screen is shown
- **THEN** the cube with its round camera button is the single most prominent action and the other features are shown as a row of smaller icons of equal weight

#### Scenario: Tap the cube to scan
- **WHEN** the user taps the cube on the home screen
- **THEN** the video scan opens

#### Scenario: Turn the cube
- **WHEN** the user drags the cube on the home screen
- **THEN** the cube turns with the finger and no scan opens, and it resumes turning on its own after the user lets go

#### Scenario: Open manual input
- **WHEN** the user taps "enter colours by hand"
- **THEN** the manual input screen opens on the front face

#### Scenario: Open the scan
- **WHEN** the user taps the round camera button
- **THEN** the video scan opens

#### Scenario: Open lessons
- **WHEN** the user taps "learn"
- **THEN** the lessons list opens

#### Scenario: Open the timer
- **WHEN** the user taps "timer and statistics"
- **THEN** the timer opens with a scramble

#### Scenario: Open the free cube
- **WHEN** the user taps "free cube"
- **THEN** the free cube screen opens

#### Scenario: Little room in the browser
- **WHEN** the home screen is shown in a phone browser whose bars take much of the height
- **THEN** the cube is drawn smaller in the space between the name and the icon row, and no text is covered

#### Scenario: Open patterns
- **WHEN** the user taps "patterns"
- **THEN** the target picker opens for a cube that is solved now

## REMOVED Requirements

### Requirement: Solve summary on the home screen
**Reason**: The user finds the best-time line on the home screen useless (2026-10-06); less on the
home screen.
**Migration**: The best time and the solve count stay in the timer's statistics and history.

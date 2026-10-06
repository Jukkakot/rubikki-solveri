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

### Requirement: Screens fit without scrolling
In portrait, every screen with actions (solution in both methods including camera follow, free
cube, scan, timer, lessons) SHALL show all its content and actions without scrolling, in the phone
app and in the phone's browser. Spacing and secondary text SHALL be compact; the screen's big
element (3D cube, camera view, timer area, lesson picture) SHALL take the height that is left and
SHALL NOT grow larger than its full width allows. During a solution the cube's size SHALL NOT
change from move to move. Only when the big element would become too small to use SHALL the screen
scroll instead. Settings and About MAY scroll; landscape is unchanged. The learn method's
solution screen MAY scroll on short screens (its stage card leaves the cube too little height).

#### Scenario: Short browser window
- **WHEN** the solution screen is shown in a phone browser with about 560 dp of height
- **THEN** the move in words and the "Done" button are visible without scrolling, and the cube is smaller than the screen width

#### Scenario: Tall phone
- **WHEN** the solution screen is shown on a tall phone where everything fits at full width
- **THEN** the cube is as wide as before

#### Scenario: Same size every move
- **WHEN** the user steps through the moves of a solution
- **THEN** the cube keeps the same size

#### Scenario: Very short screen
- **WHEN** the leftover height for the big element would be under 200 dp
- **THEN** the screen scrolls and the element keeps a usable size

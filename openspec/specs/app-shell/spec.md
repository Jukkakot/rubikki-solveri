# app-shell Specification

## Purpose
The frame of the app: the home screen that leads to every feature, the settings, and the look and
language the whole app follows.

## Requirements

### Requirement: Home screen
The app SHALL open on a home screen that shows the app name and an entry for each main feature:
scan the cube, enter colours by hand, and the free cube. An entry for a feature that is not built
yet SHALL be visibly disabled and marked as coming later.

#### Scenario: App starts on home
- **WHEN** the user opens the app
- **THEN** the home screen with the app name is shown

#### Scenario: Unbuilt feature
- **WHEN** a feature such as the camera scan is not built yet
- **THEN** its entry is disabled and labelled as coming later

#### Scenario: Open manual input
- **WHEN** the user taps "enter colours by hand"
- **THEN** the manual input screen opens on the front face

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

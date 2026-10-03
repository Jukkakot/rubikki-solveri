# about Specification

## Purpose
Tells the user which version they run, that their data stays on the phone, and which open-source
parts the app uses under which licences.

## Requirements

### Requirement: About screen
Settings SHALL lead to an about screen showing the app name, the version and a one-line
description that the app scans the cube and teaches solving it with everything kept on the phone.
The open-source parts and their licences, including min2phase's full MIT licence text, SHALL be
shown only after the user taps an "Open-source licences" button on that screen; the screen without
them SHALL fit a phone in portrait without scrolling.

#### Scenario: About at a glance
- **WHEN** the user opens Settings → About
- **THEN** the name, the version and the description are shown, and no licence text

#### Scenario: Licences shown
- **WHEN** the user taps "Open-source licences" on the about screen
- **THEN** the min2phase entry and its MIT licence text are shown

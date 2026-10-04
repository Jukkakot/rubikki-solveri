# about Specification

## Purpose
Tells the user which version they run, that their data stays on the phone, and which open-source
parts the app uses under which licences.

## Requirements

### Requirement: About screen
Settings SHALL lead to an about screen showing the app name, the version and a one-line
description that the app scans the cube and teaches solving it with everything kept on the phone.
The screen SHALL have a "download the latest version" button that opens the fixed download address
of the latest build in the phone's browser; the app itself SHALL make no network request.
The open-source parts and their licences, including min2phase's full MIT licence text, SHALL be
shown only after the user taps an "Open-source licences" button on that screen; the screen without
them SHALL fit a phone in portrait without scrolling.

#### Scenario: About at a glance
- **WHEN** the user opens Settings → About
- **THEN** the name, the version, the description and the download button are shown, and no licence text

#### Scenario: Download the latest version
- **WHEN** the user taps "download the latest version" on the about screen
- **THEN** the phone's browser opens the fixed download address of the latest build

#### Scenario: Licences shown
- **WHEN** the user taps "Open-source licences" on the about screen
- **THEN** the min2phase entry and its MIT licence text are shown

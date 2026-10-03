# about Specification

## Purpose
Tells the user which version they run, that their data stays on the phone, and which open-source
parts the app uses under which licences.

## Requirements

### Requirement: About screen
Settings SHALL lead to an about screen showing the app name, the version, a sentence that nothing
leaves the phone, and the open-source parts with their licences, including min2phase's full MIT
licence text.

#### Scenario: Licences shown
- **WHEN** the user opens Settings → About
- **THEN** the min2phase entry and its MIT licence text are shown

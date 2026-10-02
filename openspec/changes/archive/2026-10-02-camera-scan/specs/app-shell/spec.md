## MODIFIED Requirements

### Requirement: Home screen
The app SHALL open on a home screen that shows the app name and an entry for each main feature:
scan the cube, enter colours by hand, and the free cube. An entry for a feature that is not built
yet SHALL be visibly disabled and marked as coming later.

#### Scenario: App starts on home
- **WHEN** the user opens the app
- **THEN** the home screen with the app name is shown

#### Scenario: Open manual input
- **WHEN** the user taps "enter colours by hand"
- **THEN** the manual input screen opens on the front face

#### Scenario: Open the scan
- **WHEN** the user taps "scan the cube"
- **THEN** the scan screen opens asking for the front face

#### Scenario: Unbuilt feature
- **WHEN** a feature is not built yet
- **THEN** its entry is disabled and labelled as coming later

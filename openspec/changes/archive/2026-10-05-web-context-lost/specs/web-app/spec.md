# Spec Delta

## ADDED Requirements

### Requirement: Recovering from lost graphics
When the browser takes away the page's graphics (typically after the tab was in the background for
a while), the browser app SHALL recover by reloading itself silently once the page is visible, back
on the screen it was showing, instead of failing. State inside that screen (solve progress, a scan
in progress) MAY be lost. The reload SHALL be noted in the log, SHALL NOT show any notice and SHALL
NOT count as a crash. The app SHALL NOT reload itself automatically more than once in 30 seconds.

#### Scenario: Back from a long background stay
- **WHEN** the user returns to the tab after the browser took away its graphics in the background
- **THEN** the page reloads on the same screen without an error and without a "crashed" notice

#### Scenario: Graphics lost while visible
- **WHEN** the graphics are lost while the page is on screen
- **THEN** the page reloads at once on the same screen

#### Scenario: Repeated loss
- **WHEN** the graphics are lost again within 30 seconds of an automatic reload
- **THEN** the loss is logged and the page does not reload itself again

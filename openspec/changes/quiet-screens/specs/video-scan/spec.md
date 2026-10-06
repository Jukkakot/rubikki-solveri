# Spec Delta

## MODIFIED Requirements

### Requirement: Guided scan stays
The guided scan SHALL stay available. The video scan SHALL be the default: every new scan SHALL
start as the video scan. The video scan screen's menu SHALL offer an entry that switches to the
guided scan, and the guided scan screen's menu an entry that switches back to the video scan;
switching SHALL replace the screen, so going back leads to where the scan was started from.
Rescanning a single face from the colour check SHALL use the guided scan.

#### Scenario: Choosing the way
- **WHEN** the user starts scanning from the home screen
- **THEN** the video scan opens

#### Scenario: Switch to one face at a time
- **WHEN** the user picks "one picture at a time" from the video scan's menu
- **THEN** the guided scan opens in its place, and going back returns to the home screen

#### Scenario: Switch back to video
- **WHEN** the user picks "video" from the guided scan's menu
- **THEN** the video scan opens in its place

#### Scenario: Rescan one face
- **WHEN** the user rescans one face from the colour check
- **THEN** the guided scan opens for that face only

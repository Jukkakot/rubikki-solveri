## ADDED Requirements

### Requirement: Marks can be hidden
Settings SHALL offer to hide the scan's marks on the camera picture, so a screen recording of a scan
shows the cube as the camera saw it. With it on, the video scan SHALL draw no veils, rings, dots,
outlines or ticks on the camera picture; the progress ring, the status line, the turn demo and the
vibrations SHALL stay. It SHALL be off by default and remembered across starts. It SHALL change only
what is drawn, never what the scan reads or decides.

#### Scenario: Clean screen recording
- **WHEN** the user turns on hiding the marks and scans the cube
- **THEN** the camera picture shows no marks, while the progress ring fills and the scan finishes as usual

#### Scenario: Default
- **WHEN** the app is used without changing the setting
- **THEN** the marks are shown

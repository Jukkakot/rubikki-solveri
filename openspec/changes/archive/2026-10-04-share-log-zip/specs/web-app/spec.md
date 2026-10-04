## MODIFIED Requirements

### Requirement: Sharing the log in the browser
The log screen's share action SHALL open the device's share sheet with the log file and the newest
nine scan pictures (browsers share at most ten files) when the browser can share files. When the browser cannot share files or refuses the
share (other than the user cancelling it), one zip file with the log and the scan pictures SHALL
be downloaded instead. The outcome and the browser's refusal SHALL be written to the log. The
newest 12 scan pictures SHALL be kept in the browser.

#### Scenario: Share on the phone's browser
- **WHEN** the user taps share on the log screen in Chrome on Android
- **THEN** the share sheet opens with the log file and the newest nine scan pictures

#### Scenario: Share on a computer
- **WHEN** the user taps share on the log screen in a desktop browser that cannot share files
- **THEN** a zip with the log and the scan pictures is downloaded

#### Scenario: Browser refuses the share
- **WHEN** the browser says it can share files but refuses the share
- **THEN** a zip with the log and the scan pictures is downloaded and the refusal is written to the log

#### Scenario: User cancels
- **WHEN** the user closes the share sheet without choosing an app
- **THEN** nothing is downloaded

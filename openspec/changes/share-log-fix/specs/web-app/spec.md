## MODIFIED Requirements

### Requirement: Sharing the log in the browser
The log screen's share action SHALL open the device's share sheet with the log file and the scan
pictures when the browser can share files; otherwise it SHALL download the log as a text file. When
the browser refuses the share (other than the user cancelling it), the log SHALL be shared on its
own, and if that is refused too, downloaded as a text file. The outcome SHALL be written to the log.
The newest 12 scan pictures SHALL be kept in the browser.

#### Scenario: Share on the phone's browser
- **WHEN** the user taps share on the log screen in Chrome on Android
- **THEN** the share sheet opens with the log file and the scan pictures

#### Scenario: Share on a computer
- **WHEN** the user taps share on the log screen in a desktop browser that cannot share files
- **THEN** the log is downloaded as a text file

#### Scenario: Browser refuses the share
- **WHEN** the browser says it can share files but refuses the share with the pictures and with the log alone
- **THEN** the log is downloaded as a text file and the refusal is written to the log

#### Scenario: User cancels
- **WHEN** the user closes the share sheet without choosing an app
- **THEN** nothing is downloaded

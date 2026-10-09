## ADDED Requirements

### Requirement: Video scan recordings
Every video scan SHALL record what the scan was given: for each picture read, its time from the
start of the scan and every face found in it with its place in the picture and its nine colours
as read, and at the end how the scan ended (finished with which cube, left, restarted). The
recording SHALL be enough to replay the scan's decisions exactly, without the camera picture. The
newest three recordings SHALL be kept on the device. A recording longer than 90 seconds SHALL keep its last
90 seconds and say that its start is cut. Sharing the log SHALL send the recordings with the log file
and the scan pictures, and clearing the log SHALL delete them. Recordings SHALL never leave the
device except when the user shares the log. Recording SHALL not slow the scan noticeably.

#### Scenario: Failed scan kept
- **WHEN** a video scan runs for 30 seconds without finishing and the user leaves it
- **THEN** a recording of those 30 seconds, ending with "left", is kept

#### Scenario: Shared with the log
- **WHEN** the user shares the log after two video scans
- **THEN** the share gets the log file, the scan pictures and both recordings

#### Scenario: Only the newest kept
- **WHEN** a fourth video scan ends
- **THEN** the oldest recording is deleted and three remain

#### Scenario: Long scan cut
- **WHEN** a video scan runs for three minutes
- **THEN** its recording holds the last 90 seconds and says its start is cut

#### Scenario: Clear removes recordings
- **WHEN** the user clears the log
- **THEN** the recordings are deleted too

#### Scenario: Replays as on the phone
- **WHEN** a shared recording is replayed through the scan on a computer
- **THEN** the scan reaches the same known stickers and the same end as the log of that scan on the phone shows

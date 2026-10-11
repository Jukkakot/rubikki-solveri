## MODIFIED Requirements

### Requirement: Video scan log
While the video scan runs, the log SHALL get a snapshot every couple of seconds of what the scan
knows (per side, the best cube so far with its unsure stickers, how sure it is, brightness, faces
per frame, time per frame, frames read per second, whether the torch is on, how much darker the
camera has been set) and a line for each turning point: a side done, the cube clear, a stall and its
reason, a restart, leaving the screen, the camera's exposure locked. On the phone and in the browser
the snapshot SHALL also tell how long making a picture ready for finding faces took and how long finding faces
waited between pictures, both averaged since the last snapshot. When the camera opens, one line
SHALL tell which camera it is and what it can do (its resolution, focus modes, how much darker or
brighter it can be set, whether it can measure light at a point, torch), on the phone and in the
browser. A minute of scanning SHALL stay at a few dozen lines.

#### Scenario: Stuck scan readable from the log
- **WHEN** a video scan stalls and the user shares the log
- **THEN** the log shows the last snapshots with the cube read so far and the stall's reason

#### Scenario: Camera's abilities in the log
- **WHEN** the user opens the video scan in the phone's browser and shares the log
- **THEN** the log names the camera and says whether it can focus and measure light at a point and how far its exposure can be lowered

#### Scenario: Where the browser's time goes
- **WHEN** the user scans in the phone's browser and shares the log
- **THEN** each snapshot shows the time to make a picture ready, the finder's wait between pictures, the finder's time and the pictures a second

#### Scenario: Where the phone app's time goes
- **WHEN** the user scans in the phone app and shares the log
- **THEN** each snapshot shows the time to make a picture ready, the finder's wait between pictures, the finder's time and the pictures a second

## ADDED Requirements

### Requirement: Video scan log
While the video scan runs, the log SHALL get a snapshot every couple of seconds of what the scan
knows (per side, the best cube so far with its unsure stickers, how sure it is, brightness, faces
per frame, time per frame) and a line for each turning point: a side done, the cube clear, a stall
and its reason, a restart, leaving the screen. A minute of scanning SHALL stay at a few dozen lines.

#### Scenario: Stuck scan readable from the log
- **WHEN** a video scan stalls and the user shares the log
- **THEN** the log shows the last snapshots with the cube read so far and the stall's reason

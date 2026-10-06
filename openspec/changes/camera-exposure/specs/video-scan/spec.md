## ADDED Requirements

### Requirement: Camera set for the cube
During the video scan the camera SHALL measure the light and focus where the cube is, not on the
whole picture, once a face is found. While the stickers read washed out (too bright to tell their
colours), the camera SHALL be made darker step by step; exposure and white balance SHALL be locked
only once the stickers read well, or when the camera can be made no darker. Turning the torch on or
off SHALL go through the same steps again. The camera's pictures SHALL be read about fifteen times a
second where the device keeps up, and never fewer than before.

#### Scenario: Torch in a dark room
- **WHEN** the user scans in a dark room with the torch on and the cube fills only part of the picture
- **THEN** the stickers are not washed out once the scan has settled, and red and orange are told apart

#### Scenario: Cube moved to another part of the picture
- **WHEN** the cube is first found at the edge of the picture and then held in the middle
- **THEN** the camera stays focused on the cube and the readings stay sharp

#### Scenario: Camera that cannot be made darker
- **WHEN** the camera offers no way to lower its exposure
- **THEN** the scan locks as before and goes on, and washed-out readings count little

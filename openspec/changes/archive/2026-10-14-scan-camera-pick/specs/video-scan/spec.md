## MODIFIED Requirements

### Requirement: Camera set for the cube
During the video scan the camera SHALL measure the light and focus where the cube is, not on the
whole picture, once a face is found; the point SHALL be the middle of the cube as seen (all faces
found), so it does not jump from face to face as the cube turns. While the stickers read washed out
(too bright to tell their colours), the camera SHALL be made darker step by step, each step judged
only once the picture has changed after it or after a longer wait, so a slowly adjusting camera
does not get extra steps; exposure and white
balance SHALL be locked once the stickers read well, or when the camera can be made no darker, and
in any case about a second after the first face was found unless a darkening step is still under
way, however the cube moves meanwhile. When, after the lock, the stickers stay clearly darker than at the
lock for about a second, the lock SHALL be undone and the camera set one step lighter (or left to
measure by itself when it was not set darker), and it SHALL not be made darker than that again until
the torch is turned on or off. No picture SHALL be held back while the camera adjusts:
pictures SHALL be read from the first face on. While a face is found but no sticker has been read
yet, a small spinner SHALL show on the picture. Turning the torch on or off SHALL go through the
same steps again. Every picture the camera delivers SHALL be offered for reading, with no limit of
the app's own; a picture that arrives while the previous one is still being read SHALL be dropped,
so reading never falls behind the camera. A browser that cannot read off the page's thread SHALL
keep about fifteen a second, so the page stays responsive.

#### Scenario: Torch in a dark room
- **WHEN** the user scans in a dark room with the torch on and the cube fills only part of the picture
- **THEN** the stickers are not washed out once the scan has settled, and red and orange are told apart

#### Scenario: Cube moved to another part of the picture
- **WHEN** the cube is first found at the edge of the picture and then held in the middle
- **THEN** the camera stays focused on the cube and the readings stay sharp

#### Scenario: Camera that cannot be made darker
- **WHEN** the camera offers no way to lower its exposure
- **THEN** the scan locks as before and goes on, and washed-out readings count little

#### Scenario: As fast as the phone allows
- **WHEN** the camera delivers 30 pictures a second and each is read in less than a thirtieth of a second
- **THEN** about 30 pictures a second are read, as the log's snapshot shows

#### Scenario: Cube turned in the hand from the start
- **WHEN** the user turns the cube in the hand from the moment it is first found, two faces in view, in normal light
- **THEN** the first stickers are read from the first pictures with a face, and the camera locks about a second after the first face was found

#### Scenario: Spinner until the first reading
- **WHEN** a face has been found and no sticker has been read yet
- **THEN** a small spinner shows on the picture, and it goes away as soon as the first sticker is read

#### Scenario: Picture turns dark after the lock
- **WHEN** the camera was made two steps darker and locked, and the cube's stickers then read at about half the brightness they had at the lock
- **THEN** about a second later the camera is unlocked, set one step lighter and locked again, and it does not go back to the darker step

#### Scenario: Slow camera gets no extra steps
- **WHEN** the stickers read washed out and the camera takes longer than usual to darken after a step
- **THEN** the next step waits until the picture has changed, or a longer time has passed

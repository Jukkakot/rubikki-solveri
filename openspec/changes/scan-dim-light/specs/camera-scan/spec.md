## MODIFIED Requirements

### Requirement: Live reading
While a face is in the grid, each cell SHALL show a dot of the colour the camera currently sees
there, as seen, without deciding which cube colour it is. The screen SHALL say which face the centre
looks like, among the faces not yet scanned; this SHALL NOT stop the capture. The reading of the
centre SHALL use the colours this cube has already shown (the accepted centres) and a default
palette for colours not seen yet, and SHALL compare colours regardless of how bright they read, so
that a dim light does not make a dark colour look like white.

#### Scenario: Wrong face
- **WHEN** no face is done yet and the camera sees a red centre
- **THEN** the screen says the centre looks like the right face, and the face can be captured

#### Scenario: Warm red
- **WHEN** the cube's red reads closer to the default orange and the right face (red centre) is shown
- **THEN** the face can be captured and its recognised face changed to the right face in the review, and once accepted, this cube's red reads as red

#### Scenario: Raw colours
- **WHEN** a cell of the grid sees a pinkish red
- **THEN** its dot shows that pinkish red, not a palette colour

#### Scenario: Dark blue in dim light
- **WHEN** no face is done yet and the centre reads a very dark blue (as in the user's evening scan of 2026-10-04)
- **THEN** the screen says the centre looks like the back face (blue), not the top face

### Requirement: Classification by the centres
Each sticker's reading SHALL be the average colour of most of the sticker's middle, leaving out the
lightest and darkest parts, so that a highlight or a dark corner does not decide it. After all
faces, each sticker SHALL get the colour of the centre it is closest to, such that every colour is
used exactly nine times, and a confidence. The six centres SHALL keep their colours.

#### Scenario: Different lighting
- **WHEN** a scrambled cube is scanned under warm indoor light that shifts all colours
- **THEN** every sticker gets its right colour

#### Scenario: Doubtful sticker
- **WHEN** a sticker's reading is about equally close to two colours
- **THEN** it is marked uncertain

#### Scenario: Highlight on a sticker
- **WHEN** a lamp's reflection covers a small part of a sticker's middle
- **THEN** the sticker's reading is its own colour, not the reflection

### Requirement: Result
After the six faces, the scan SHALL name the six centres' colours together, as the one assignment of
the six colours to the six centres that fits their readings best, and SHALL rename faces silently
where this differs from what was recognised during the scan. It SHALL then find how each face was
turned, on the assumption that the real cube is solvable: of all rotations of the six faces, the one
that gives a solvable cube SHALL be used. When none does, the next-best namings of the centres SHALL
be tried in order of fit, and the first that gives a solvable cube SHALL be used. When still none
does, the one with the most stickers forming real pieces SHALL be used, and if the colours of two
opposite centres were read the wrong way round, swapping them SHALL be tried too. When rotations
giving different solvable cubes exist, the faces whose rotation differs SHALL be marked. After every
scan the check SHALL open with the camera's pictures next to the scanned colours. A valid scan where
no sticker is uncertain or marked SHALL show no marks and a short note to compare with the pictures
and go on; otherwise the uncertain or problem stickers SHALL be marked, with a note asking the user
to check them.

#### Scenario: Confident scan
- **WHEN** the scan is valid and confident
- **THEN** the check opens with the pictures and colours, nothing marked, and "Looks right" opens the solution

#### Scenario: Faces turned
- **WHEN** a scrambled cube is scanned with the top face turned a quarter and the back face upside down
- **THEN** the check shows the real cube, and "Looks right" opens its solution

#### Scenario: Unsure scan
- **WHEN** the scan has uncertain stickers or is invalid
- **THEN** the check opens with those stickers marked

#### Scenario: Face recognised wrong during the scan
- **WHEN** the blue face was recognised as the top during a dim scan and the white face got the remaining name (the user's evening scan of 2026-10-04)
- **THEN** the faces are renamed without a message, and the check shows a valid cube

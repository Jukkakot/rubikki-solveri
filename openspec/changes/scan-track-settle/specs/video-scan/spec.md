## MODIFIED Requirements

### Requirement: Recognised by agreement
The scan SHALL keep track of the cubes that are still possible and narrow them with every
observation, using only facts that hold for every real cube: each colour on nine stickers, each
centre's colour fixed with its opposite (white–yellow, green–blue, red–orange), only real corner and
edge pieces, each piece once, the corners' twist, the edges' flip and the parity of a real cube.
Which face and turn a reading shows SHALL be among the possibilities, not decided before them by
how its centre looks: faces found in one picture SHALL be different faces that are neighbours on
the cube (never opposite colours), touching along the edges the picture shows them touching, which
also fixes their turns and the order of the colours round a corner; a face followed from picture to
picture SHALL stay the same face; how a centre looks SHALL count only as evidence, never as a rule.
A sticker SHALL count as known only when every cube still clearly possible has the same colour there,
whether from several agreeing frames or from the rest of the cube. A single wrong frame SHALL NOT
change a known sticker or finish the scan; readings that disagree with each other SHALL weigh
against each other and recent clear readings SHALL count over old ones, so that a face read wrong at
first is put right by later clear views. Red and orange readings SHALL count as weaker evidence
against each other than other colours. Renaming or re-turning a face SHALL NOT forget what its
stickers were read as. The scan's view of which face and turn each reading shows SHALL change
only when something new speaks for it (a new reading, or a reading growing old): while nothing new
is read it SHALL stay the same, and it SHALL NOT go back and forth between two views picture after
picture.

#### Scenario: One bad frame
- **WHEN** one frame reads a sticker wrong among many that read it right
- **THEN** the sticker keeps its right colour

#### Scenario: Known from the others
- **WHEN** two stickers of a corner are known and its third was never seen
- **THEN** the third is known from the corner's colours

#### Scenario: Impossible piece
- **WHEN** a sticker reads yellow next to a white sticker of the same piece
- **THEN** the reading counts only for the colours a real piece allows there, and yellow is never shown for it

#### Scenario: Dark centre taken for another colour
- **WHEN** the white face and the blue face are in view together and the blue face's centre, in shadow, reads closer to white
- **THEN** the white face is read from the white face only, and the blue face's readings count for the blue face

#### Scenario: Doubtful centre
- **WHEN** a face is seen whose centre fits white and blue about equally, and no other view settles which it is
- **THEN** its stickers are not shown as known until a view settles it

#### Scenario: Orange face first
- **WHEN** the scan starts with the orange face in view, its centre fitting red and orange about equally, and the red face is shown only later
- **THEN** no sticker of the red side is shown wrong at any time, and once the red face is seen both are named right

#### Scenario: Red centre looks orange
- **WHEN** the white, yellow, green, blue and orange faces are known and the red face, whose centre looks more orange than red, is shown several times
- **THEN** the red face is told red, its stickers become known and the scan can finish

#### Scenario: Look-alike centres
- **WHEN** the red centre looks orange in this light, and the red and orange faces are each shown, never together
- **THEN** the two faces are told apart by the faces seen around them, and the scan finishes with the true cube

#### Scenario: Corner decides red or orange
- **WHEN** a corner with the white and green faces is in view and its third centre could be red or orange by its look
- **THEN** the third face is named by which way round the three faces run, and the reading's stickers count for that face

#### Scenario: Neighbours are never opposite
- **WHEN** two faces are found side by side whose centres look white and pale yellow
- **THEN** they are never taken as the white and the yellow face together

#### Scenario: Patterned cube
- **WHEN** a pattern makes one face, turned, look like another face (a striped cube)
- **THEN** the faces are still told apart and the scan finishes with the true cube

#### Scenario: Blue face first, white face later
- **WHEN** the scan starts with the blue face on top, its centre reading nearer white, and the white face is shown only later
- **THEN** no sticker is shown wrong at any time, and once the white face is seen the blue face is named blue

#### Scenario: Face read wrong at first
- **WHEN** a face was known wrong from a long run of bad readings at the start, and the user then shows it to the camera for a few seconds
- **THEN** its stickers change to what the clear views show, and the scan can finish

#### Scenario: Rename keeps the stickers
- **WHEN** a face's name or turn changes as more is seen
- **THEN** the stickers whose readings still agree stay known, and the number of known stickers does not drop

#### Scenario: Nothing new read
- **WHEN** the cube is out of view, or a face has left the picture, and no new reading arrives
- **THEN** the shown stickers, the ring and the faces' names and turns stay as they are

#### Scenario: Held still
- **WHEN** the cube is held still in view for a few seconds
- **THEN** no sticker switches back and forth between two colours picture after picture

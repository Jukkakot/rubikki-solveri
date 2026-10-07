## MODIFIED Requirements

### Requirement: Recognised by agreement
A sticker SHALL count as known only when several frames agree on its colour, or when the rest of
the cube leaves only one possible colour for it. A single wrong frame SHALL NOT change a known
sticker or finish the scan. Red and orange readings SHALL count as weaker evidence against each
other than other colours. Faces SHALL be told apart by how their centres look on this cube in this
light, compared with each other, not only against fixed reference colours: two faces whose centres
look clearly different SHALL never be taken for the same face, even when both are nearer the same
reference colour and are never in view together. The faces seen so far SHALL be named together,
each colour once, and when five have been seen the sixth SHALL follow. While a face's centre fits
two colours about equally and the naming cannot tell them apart yet, its stickers SHALL NOT be shown
as known. Two faces found in the same picture SHALL never be taken for the same face: when their
centres name the same colour, the one that fits it better keeps it and the other takes its
next-best colour if it fits nearly as well. Recent readings SHALL count over old ones: when the
readings of a face seen for a while agree with each other and outnumber what was read before, they
SHALL replace known stickers, so that a face read wrong at first is put right by later clear views.

#### Scenario: One bad frame
- **WHEN** one frame reads a sticker wrong among many that read it right
- **THEN** the sticker keeps its right colour

#### Scenario: Known from the others
- **WHEN** two stickers of a corner are known and its third was never seen
- **THEN** the third is known from the corner's colours

#### Scenario: Dark centre taken for another colour
- **WHEN** the white face and the blue face are in view together and the blue face's centre, in shadow, reads closer to white
- **THEN** the white face is read from the white face only, and the blue face's readings count for the blue face

#### Scenario: Blue face first, white face later
- **WHEN** the scan starts with the blue face on top, its centre reading nearer white, and the white face is shown only later
- **THEN** the two faces' readings are never mixed, and once the white face is seen the blue face is named blue

#### Scenario: Doubtful centre
- **WHEN** a face is seen whose centre fits white and blue about equally, and no other face settles which it is
- **THEN** its stickers are not shown as known until the naming is clear

#### Scenario: Orange face first
- **WHEN** the scan starts with the orange face in view, its centre fitting red and orange about equally, and the red face is shown only later
- **THEN** no sticker of the red side is shown wrong at any time, and once the red face is seen both are named right

#### Scenario: Face read wrong at first
- **WHEN** a face was known wrong from a long run of bad readings at the start, and the user then shows it to the camera for a few seconds
- **THEN** its stickers change to what the clear views show, and the scan can finish

### Requirement: Reading in different light
The video scan SHALL read colours the same in warm, cool or dim light as in daylight as far as the
picture allows: a sticker's shine SHALL not change its colour, a reading washed out by too much light SHALL count
little, and a reading between two colours SHALL count as uncertain between them rather than as a
sure one, so that the rest of the cube decides. A sticker SHALL be named by its colour more than by
its brightness, so that a colour seen in dimmer or brighter light than its face's centre is still
named right, and a centre that in its light looks like another colour SHALL NOT be the reference for
its own colour. Turning the torch on or off SHALL make the camera adjust to the new light while
reading goes on. A cube read in poor light SHALL still never finish as a wrong cube.

#### Scenario: Warm ceiling light
- **WHEN** the cube is scanned under a dim warm ceiling light in which red looks orange-ish
- **THEN** the scan finishes with the true cube

#### Scenario: Shine on a sticker
- **WHEN** a lamp's reflection lies on part of a sticker
- **THEN** the sticker is read in its own colour

#### Scenario: Torch turned on during the scan
- **WHEN** the user turns the torch on after the scan has started
- **THEN** the picture is not washed out once the camera has adjusted, reading does not pause, and orange is not read as yellow nor blue as white

#### Scenario: Face seen in another light
- **WHEN** the yellow face's centre was seen in bright light, washed out to near white, and the yellow stickers of another face are seen in dimmer light
- **THEN** those stickers are read yellow, not green, and the scan finishes with the true cube

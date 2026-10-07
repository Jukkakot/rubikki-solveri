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

#### Scenario: Face read wrong at first
- **WHEN** a face was known wrong from a long run of bad readings at the start, and the user then shows it to the camera for a few seconds
- **THEN** its stickers change to what the clear views show, and the scan can finish

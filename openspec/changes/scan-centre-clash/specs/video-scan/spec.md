## MODIFIED Requirements

### Requirement: Recognised by agreement
A sticker SHALL count as known only when several frames agree on its colour, or when the rest of
the cube leaves only one possible colour for it. A single wrong frame SHALL NOT change a known
sticker or finish the scan. Red and orange readings SHALL count as weaker evidence against each
other than other colours. Two faces found in the same picture SHALL never be taken for the same
face: when their centres name the same colour, the one that fits it better keeps it and the other
takes its next-best colour. Readings of a face seen straight on SHALL count more than readings of
a face seen at a steep angle. Recent readings SHALL count over old ones: when the readings of a face
seen for a while agree with each other and outweigh what was read before, they SHALL replace known
stickers, so that a face read wrong at first is put right by later clear views.

#### Scenario: One bad frame
- **WHEN** one frame reads a sticker wrong among many that read it right
- **THEN** the sticker keeps its right colour

#### Scenario: Known from the others
- **WHEN** two stickers of a corner are known and its third was never seen
- **THEN** the third is known from the corner's colours

#### Scenario: Dark centre taken for another colour
- **WHEN** the white face and the blue face are in view together and the blue face's centre, in shadow, reads closer to white
- **THEN** the white face is read from the white face only, and the blue face's readings count for the blue face

#### Scenario: Face read wrong at first
- **WHEN** a face was known wrong from a long run of bad readings at the start, and the user then shows it straight to the camera for a few seconds
- **THEN** its stickers change to what the clear views show, and the scan can finish

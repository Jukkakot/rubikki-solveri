## MODIFIED Requirements

### Requirement: Hold step
After "Aloita" and before the guide, a hold step SHALL show "Pidä kuutiota näin": the user's own cube in the
holding position with rings around the front, right and top centre pieces and no hands, the hold in words
(front, right and top colour, with the same side names as the move words), a hint to turn the cube a little to
see the right side, a line that the centre piece tells the side's colour, and one primary button "Olen
valmis". Only "Olen valmis" SHALL open the guide, at the first move in the manual mode.

#### Scenario: Picture of the hold
- **WHEN** the hold step is open
- **THEN** the cube is seen as in the guide, big, with no hands drawn, it does not turn when dragged, and it reads on a light and a dark background

#### Scenario: Turns into the hold
- **WHEN** the hold step opens with animations on
- **THEN** the cube first turns from a tilted pose into the holding view, and the rings appear once it is there; with animations off it is shown in the hold at once

#### Scenario: Centre pieces marked
- **WHEN** the cube is in the hold
- **THEN** pulsing rings surround the front, right and top centre pieces, and the words say that the centre piece tells the side's colour

#### Scenario: Ready opens the guide
- **WHEN** the hold step is open and the user taps "Olen valmis"
- **THEN** the guide opens at move 1 and does not advance by itself

#### Scenario: Hold in the picture and in words
- **WHEN** the hold step opens for a cube with green at the front, red on the right and white on top
- **THEN** the words read "Vihreä edessä, punainen oikealla, valkoinen ylhäällä." followed by the hint to turn the cube a little to see the right side

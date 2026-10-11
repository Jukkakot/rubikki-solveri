## MODIFIED Requirements

### Requirement: Reading in different light
The video scan SHALL read colours the same in warm, cool or dim light as in daylight as far as the
picture allows: a sticker's shine SHALL not change its colour, a reading washed out by too much light SHALL count
little, and a reading between two colours SHALL count as uncertain between them rather than as a
sure one, so that the rest of the cube decides. A sticker SHALL be named by its colour more than by
its brightness, so that a colour seen in dimmer or brighter light than its face's centre is still
named right, and a centre that in its light looks like another colour SHALL NOT be the reference for
its own colour. Red and orange SHALL be told apart in each face's own light: once a face is known
and shows stickers of both that the centres would name wrong, its own red and orange stickers also
count as how the two look, so that a sticker in shade is judged against the light it is in, not
against a centre seen in brighter or other light. Turning the
torch on or off SHALL make the camera adjust to the new light while reading goes on. A cube read in
poor light SHALL still never finish as a wrong cube.

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

#### Scenario: Orange on a face in shade
- **WHEN** the green face lies in shade, so that its orange stickers look as red as the red centre did in the lamp's light, and the face also shows red stickers
- **THEN** its orange stickers are named orange, not red, and the scan finishes with the true cube

#### Scenario: Red and orange alike on every face
- **WHEN** no known face yet shows both red and orange stickers
- **THEN** red and orange are named as before, against the cube's own centres, and the scan does not finish any sooner or as a different cube than it would have

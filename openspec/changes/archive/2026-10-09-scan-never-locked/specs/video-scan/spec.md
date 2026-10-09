## ADDED Requirements

### Requirement: Nothing locked
No interpretation in the video scan SHALL be final until the scan finishes. This covers which face a
followed face is, how it is turned, what its centre is named and what a sticker is read as. Whenever
the cube is not yet clear and a new reading arrives, the scan SHALL recheck the faces' turns
together, every way the faces can be turned that the pictures allow, not each face against the
others' current choices. A whole that makes a clearly better cube SHALL replace the current one at
once, also for faces settled or confirmed before. Which face a followed face is stays open to the
joint assignment of every picture. The change SHALL be silent: the marks and ticks change to the
better cube, with no message.

#### Scenario: Turns settled wrong together
- **WHEN** every face's colours were read right but the faces' turns were settled wrong in the first pictures, each wrong turn fitting the others
- **THEN** the scan changes all the turns together to the ones that make a real cube, and finishes with the true cube

#### Scenario: A ticked side corrected
- **WHEN** a side has a tick and a recheck finds a clearly better cube in which that side is turned differently
- **THEN** the side's stickers change to the better cube, its tick goes, and it comes back once the better cube confirms the side

#### Scenario: No change without need
- **WHEN** the cube is already clear
- **THEN** no recheck changes it, and the scan finishes as before

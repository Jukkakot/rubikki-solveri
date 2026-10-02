## MODIFIED Requirements

### Requirement: Check
When every sticker has a colour, the user SHALL be able to check the cube. An invalid cube SHALL
give a short message naming the problem in plain words and mark the stickers concerned where
known; a valid cube SHALL be accepted and its solution opened.

#### Scenario: Unfinished cube
- **WHEN** some stickers have no colour
- **THEN** the check is not available

#### Scenario: Invalid cube
- **WHEN** a cube with a twisted corner is checked
- **THEN** a message says one corner is twisted

#### Scenario: Valid cube
- **WHEN** a valid cube is checked
- **THEN** it is accepted and its solution opens

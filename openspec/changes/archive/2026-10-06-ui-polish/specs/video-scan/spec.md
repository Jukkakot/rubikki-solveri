# Spec Delta

## MODIFIED Requirements

### Requirement: Finish the video scan
The scan SHALL finish when one possible cube fits what has been read clearly better than any other
possible cube, and this holds for about half a second; not every sticker needs to have been seen.
The solution's start screen SHALL then open with a new scan behind it, as for a sure guided scan;
stickers known only from the others are marked in the colour check reached from the guide's menu.
The user SHALL be able to stop earlier and open the check with what is known. The scan SHALL never
stay with everything read and nothing happening.

#### Scenario: Whole cube seen
- **WHEN** the cube that fits the readings is clear for half a second
- **THEN** the start screen opens, and going back starts a new scan

#### Scenario: Orange read as red
- **WHEN** one orange sticker has been read as red, so no real piece fits it
- **THEN** the scan takes the piece that fits the rest and finishes

#### Scenario: Stop early
- **WHEN** the user stops the video scan with stickers still unknown
- **THEN** the check opens with the known colours and the unknown stickers marked

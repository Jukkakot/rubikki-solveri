## ADDED Requirements

### Requirement: Reading in different light
The video scan SHALL read colours the same in warm, cool or dim light as in daylight as far as the
picture allows: the colour cast of the light SHALL be taken out using the cube's white stickers, a
sticker's shine SHALL not change its colour, and a reading between two colours SHALL count as
uncertain between them rather than as a sure one, so that the rest of the cube decides. A cube read
in poor light SHALL still never finish as a wrong cube.

#### Scenario: Warm ceiling light
- **WHEN** the cube is scanned under a dim warm ceiling light in which red looks orange-ish
- **THEN** the scan finishes with the true cube

#### Scenario: Shine on a sticker
- **WHEN** a lamp's reflection lies on part of a sticker
- **THEN** the sticker is read in its own colour

## MODIFIED Requirements

### Requirement: Restart with a reason
When the scan cannot make progress, the screen SHALL say why with an icon and a few words: too
dark, no cube found for a while, or no new stickers for a while with the cube in view for about
fifteen seconds (including readings that no possible cube fits). The notice SHALL lie at the bottom
of the camera picture without covering the cube, and the scan SHALL go on underneath it: the user
can always keep scanning. The notice SHALL offer to start the scan again and to go to the colour
check with what is known, and, where the device has a torch, to turn the torch on when the reason
is the light or no progress. It SHALL go away when new stickers become known, and a tap anywhere
outside it SHALL close it. Starting again SHALL clear what was read and keep the camera running;
it is offered only in the notice. Dim light SHALL also be told early, as a small notice on the
picture, before the scan stalls.

#### Scenario: Dim light
- **WHEN** the picture is too dark to read well
- **THEN** a small lamp notice is shown on the picture at once, and if the scan stalls the notice names the light as the reason and offers the torch

#### Scenario: No progress
- **WHEN** the cube is in view but nothing new is known for about fifteen seconds
- **THEN** the reason is shown at the bottom of the picture with a restart button, and restarting clears the progress

#### Scenario: Keep scanning
- **WHEN** the notice is shown and the user keeps turning the cube
- **THEN** the scan goes on, and the notice goes away as soon as new stickers become known

#### Scenario: Close the notice
- **WHEN** the user taps the picture outside the notice
- **THEN** the notice closes and the scan goes on

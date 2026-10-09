## MODIFIED Requirements

### Requirement: Corner row
During the whole video scan a row of eight small corner pictures SHALL lie above the status line,
one for each corner of the cube, each drawn as a cube corner in the colours of the three centres
that meet there. A corner SHALL count as read only once everything it stands for is sure: its three
corner stickers and the stickers of the three edges that meet at it are part of the clear cube, not
known from their own readings alone. All eight corners SHALL count as read only when the scan is
complete, so a row with every corner ticked always means the cube is done. A read corner SHALL dim
and get a tick, keeping its place, so the row shows both what is left and what is done. The corner
worth showing next SHALL pulse.

#### Scenario: From the start
- **WHEN** the video scan opens and nothing has been read yet
- **THEN** eight corner pictures are shown in their colours, none dimmed

#### Scenario: Corner read
- **WHEN** the three stickers of the white, red and green corner and the stickers of its three edges become part of the clear cube
- **THEN** that corner picture dims and gets a tick, and the others keep their places

#### Scenario: Corner stickers alone are not enough
- **WHEN** the three stickers of a corner are known but one of its edges is still in doubt
- **THEN** that corner is not ticked

#### Scenario: All ticked means done
- **WHEN** seven corners are ticked and the last open doubt touches the eighth
- **THEN** the eighth stays unticked and pulses until the scan is complete

#### Scenario: Next corner pulses
- **WHEN** three corners are still unread and the white, red and blue one would settle the most
- **THEN** the white, red and blue corner picture pulses

#### Scenario: Finished
- **WHEN** the scan finishes, also with some stickers never seen
- **THEN** every corner picture is dimmed with a tick

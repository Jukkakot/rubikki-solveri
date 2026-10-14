## MODIFIED Requirements

### Requirement: Speed in the browser
In Chrome on the reference phone, the shortest solution SHALL appear within two seconds of
opening the solution screen, and the beginner solution within one second. The one-time
preparation of the solver after start SHALL NOT delay the home screen's first appearance. The
video scan in the browser SHALL read about as many pictures a second as the phone app, with the
camera picture and the marks staying smooth, by doing its reading away from the page's drawing;
where that is not possible it SHALL work as before. The page SHALL not copy or read camera pictures that
nothing on the screen uses. A phone whose page draws fewer than about 18 frames a second during a
video scan SHALL draw the app at a lower pixel ratio (1.5) from the next start.

#### Scenario: Solve in the browser
- **WHEN** the user opens the solution of a scanned cube in the phone's browser after the app has been open a few seconds
- **THEN** the first move is shown within two seconds

#### Scenario: Video scan keeps up in the browser
- **WHEN** the user turns the cube in front of the camera in the video scan in the phone's browser
- **THEN** the picture stays smooth and the scan reads about as many pictures a second as the phone app, as the log shows

#### Scenario: Slow phone draws lighter
- **WHEN** a phone's page draws about 12 frames a second during a video scan at a pixel ratio of 3
- **THEN** after about 8 s the log says the ratio is capped, and from the next start the app draws at ratio 1.5 and the scan reads more pictures a second

## MODIFIED Requirements

### Requirement: Camera in the browser
The scan and camera follow SHALL use the device's back camera when there is one, else any camera.
When the device has several back cameras and the browser's choice has no torch, the other back
cameras SHALL be tried once and the first with a torch used; the camera chosen SHALL be remembered
for later scans and named in the log.
The browser's camera permission prompt SHALL be shown when the user opens the scan; if access is
denied, the screen SHALL explain how to allow the camera in the browser's site settings and offer
manual input instead. The torch button SHALL be shown only when the camera supports a torch. When
the camera supports it, exposure and white balance SHALL be locked at the first capture as on the
phone; when it does not, the scan SHALL work without the lock and the log SHALL say the lock is not
supported. In the video scan the browser camera SHALL measure light and focus on the cube and be made
darker when the stickers wash out, as on the phone, as far as the browser lets the page control the
camera; what it does not allow SHALL be skipped without harm and named in the log. The camera picture SHALL be shown as sharp and as smooth as the camera delivers it (its
own resolution, about the camera's frame rate), whatever size the colours are read at, with the
scan's marks drawn on top in the right places.

#### Scenario: Scan in the phone's browser
- **WHEN** the user opens the scan in Chrome on the phone and allows the camera
- **THEN** the back camera's picture fills the scan view with the grid and the live colour dots, and faces are captured and checked as in the phone app

#### Scenario: Sharp and smooth picture
- **WHEN** the user opens the video scan or the scan in the phone's browser
- **THEN** the camera picture is as sharp and moves as smoothly as in the phone's camera app, and the marks stay on the stickers

#### Scenario: Back camera with a torch
- **WHEN** the phone has several back cameras and the browser opens one without a torch
- **THEN** the scan switches to a back camera that has one, shows the torch button, and opens that camera directly next time

#### Scenario: Torch-lit cube in the browser
- **WHEN** the user scans in the phone's browser in a dark room with the torch on
- **THEN** the stickers are not washed out once the scan has settled, as in the phone app

#### Scenario: Camera denied
- **WHEN** the user blocks the camera for the page
- **THEN** the scan screen explains how to allow it in the browser's site settings and offers manual input

#### Scenario: No torch
- **WHEN** the camera has no torch the browser can control (e.g. a laptop webcam)
- **THEN** no torch button is shown

# Tasks

## 1. Scene

- [ ] 1.1 `CubeScene`: mirror constants and geometry (centre, normal by the reflection rule, frame and glass quads), `reflect` for quads and arrow points, a projection fit that includes the mirror for `DEFAULT_VIEW`
- [ ] 1.2 Tests (`CubeSceneTest`): reflecting twice gives the original; in the holding view the ray to the mirror's centre reflects to the cube's centre; the reflection shows the back face's stickers; the mirror quads do not change with the view while the reflection does

## 2. Drawing and guide

- [ ] 2.1 `Cube3D(mirror)`: frame, glass, reflected cube and arrow clipped to the glass, then the cube
- [ ] 2.2 `GuideCube`: the mirror card, the second cube, the 10 % shift, `MIRROR_VIEW` and `mirror_label` removed; existing guide tests updated

## 3. Check

- [ ] 3.1 `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution`; screenshots `solve`, `guide-back`, `guide-dragged` judged by eye (light and dark), placement constants tuned

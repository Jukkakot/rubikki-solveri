# Tasks

## 1. Scene

- [x] 1.1 `CubeScene`: mirror constants and geometry in camera space (centre, normal by the reflection rule, frame and glass quads), `reflect` for quads and arrow points, a projection fit that includes the mirror
- [x] 1.2 Tests (`CubeSceneTest`): reflecting twice gives the original; the ray to the mirror's centre reflects to the cube's centre; the reflection shows the back face's stickers in the holding view; the mirror quads do not change with the view while the reflection does

## 2. Drawing and guide

- [x] 2.1 `Cube3D(mirror)`: frame, glass, reflected cube and arrow clipped to the glass, then the cube
- [x] 2.2 `GuideCube`: the mirror card, the second cube, the 10 % shift, `MIRROR_VIEW` and `mirror_label` removed
- [x] 2.3 One view: `steady` removed from `GuideCube`, `MoveWordsText`, `FollowPanel` and `SolveScreen`; `guideView` and the swing removed; the learn method gets the mirror and the reset button; `SteadyGuideTest` covers the learn method (view the same over R, B, L, D)

## 3. Words

- [x] 3.1 `MoveWords`: one wording (holding-view sentences, whole-cube turns by centres); `steady` parameters and the old strings removed; lessons' algorithm demo uses it
- [x] 3.2 Strings fi/en: every text with layer or direction words aligned to the same terms (design 8); tests (`MoveWordsTest`, `SteadyWordsTest`) updated, one checking the learn method and a lesson demo use the same sentence as the fast method

## 4. Check

- [x] 4.1 `./gradlew test lint assembleDebug :web:wasmJsBrowserDistribution`; screenshots `solve`, `solve-learn`, `guide-back`, `guide-dragged`, `lesson-algorithm` judged by eye (light and dark), mirror constants tuned
- [x] 4.2 `docs/`: the guide's view and mirror lines updated where the wiki describes them

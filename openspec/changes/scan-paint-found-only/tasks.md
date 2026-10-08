# Tasks

## 1. Paint (shared)

- [x] 1.1 `ScanPaint.of`: only `state.found` is painted; the projection loop goes; a found face gets its dim outline only when `read != null` (outline + tick for a confirmed side as now). `PaintLayer`: no projection alpha or centre (the motion centre from the largest face found). Tests in `ScanPaintTest`: a projection with no face found paints nothing; a found face without `read` has no outline; replace the projection tests (`aProjectedSideWithoutReadingsIsGreyWhereNeeded`, `anOpenFaceOverAProjectedSideGetsOnlyItsOwnMarks`) accordingly

## 2. Wrap-up

- [x] 2.1 `./gradlew check`, web build and both browser smoke tests
- [x] 2.2 Docs: `docs/architecture.md` paint paragraph (found faces only; the projection kept for the pose and the turn demo) and the route table line; roadmap entry
- [x] 2.3 Install on the phone if connected; list for the user to try: no marks in the air or beside the cube, the outline only on faces read

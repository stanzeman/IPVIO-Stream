# T-057 – Search on the video portal's server

Status: implemented, not built with Gradle, not device tested. JVM tests for the pure logic ran. Builds on T-054.

## Goal
The app searches only its loaded list. A video portal can search its whole catalogue on the server (captured request: `{"cmd":"search","query":"mr bean","key":…}`, answer like a category page). Titles not loaded yet (list still loading, cut-off category, a title added since the last update) should still be findable.

## Change
- `PortalLogic.kt` (no Android): `searchQuery` (trim, at least 2 characters, at most 60), `searchItem` (hit to `Item`: movie with stream address plus `#/movie/`, series as lazy placeholder `portal://…/series/<fid>.ser`), `newHits` (without titles already in the list, no duplicates, at most 60).
- `PortalApi.kt`: `searchItems(query, have)` asks every stored portal list (at most 3), keeps answers per source and query in a small cache (40 entries) for the running app.
- `SearchScreen.kt`: after a 450 ms typing pause the portal is asked in the background; hits not in the local list appear as the last row "Im Portal gefunden". The movie/series filter of the search applies; a query with only portal hits no longer shows "No hits".

## Not done
- The portal's own filters (genre, year range: `{"filter":"on","genre":…,"years":"2021-2026"}`) are not used; the app filters its loaded list itself.
- Loading a category page by page while scrolling (as the portal's own app does) is not done; the app keeps its full list (see T-054).
- All stored portal lists are searched, also one that is not shown at the moment.

## Risks
One request per typing pause and source. With the 450 ms pause and the cache this is a few requests per search. A 429 answer is waited for once (as for all portal requests).

## Tests
`PortalLogicTest`: 32 tests (3 new) ran with JUnit outside Gradle: 32 of 32 passed. `PortalApi.kt` and `SearchScreen.kt` were not compiled here.

## Device test steps
1. Open search, type the name of a title that is surely in the portal but not (yet) in the list (or test while the list is still loading).
2. After a moment the row "Im Portal gefunden" appears. Open a movie: it plays. Open a series: its episodes load.
3. Type a title that is in the list: it appears only in the normal rows, not twice.

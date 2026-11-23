# T-054 – Video portal: load only new titles

Status: implemented, not built with Gradle, not device tested. JVM tests for the pure logic ran. Builds on T-051 (portal source, branch `feat/T-036-vportal-source`).

## Goal
Every refresh of a portal list loaded all categories again (about 440 requests and 10 to 20 minutes for the biggest category). While it ran, the movie list was incomplete and "Add" did not react (`PlaylistStore.loading`).

## How the portal behaves (from captures)
Categories list the newest titles first, 30 per page, with a `next` entry for the next offset. So new titles are always on the first pages.

## Change
- `PortalLogic.kt`: every entry carries `portal-fid="<id>"` (ignored by the list parser). The list ends with a head line `#PORTAL-LIST:4 full=<time of the last full load> vod=<movie host>`. New helpers: `head`/`parseHead`, `fidOf`, `keepLine`, `needsFull` (30 days), `pageKnown` (stop rule), `hostOf`, `hostGone`.
- `PortalApi.kt`: `write(..., old)`. With a stored list: its entries are copied first, then each category is loaded only until the first page whose titles are all known; known titles are skipped (no second film lookup). If the stored movie host no longer appears on a page, `StaleList` is thrown and everything is loaded.
- `PlaylistStore.kt`: `oldPortalList()` reads the stored compressed copy of the list (`pl_<key>.m3u.gz`, written by the normal load) twice as a stream (ids first, entries while writing), so it is never held in memory as a whole. `downloadPortal()` uses it, and on `StaleList` loads everything. `PORTAL_FORMAT` = 4, so lists of the older format are loaded in full once.

## When everything is loaded
No stored copy, older format, no ids, last full load 30 days ago (or a clock in the past), "Reload playlist completely" (deletes the stored files), or the movie host changed.

## Limits
- A title the portal removes stays until the next full load (at most 30 days).
- A title the portal adds with an old position (not at the top) is found only by the next full load.
- Storage: the list format changes (attribute and head line). Older stored lists are not converted; they are replaced by one full load. No SharedPreferences key or file name changed.

## Tests
`PortalLogicTest`: 29 tests (7 new) ran with JUnit outside Gradle: 29 of 29 passed. `PortalApi.kt` and `PlaylistStore.kt` (OkHttp, Android) were not compiled here.

## Device test steps
1. Install, open the app: the portal list is loaded in full once (phase "Kategorien werden geladen … Titel").
2. Settings > Playlists > the portal list > update (not "completely"): phase "Gespeicherte Titel werden übernommen", then "Neue Titel werden gesucht (i/n)". It should end within about a minute; the movie count stays or grows.
3. "Reload playlist completely": everything is loaded again.

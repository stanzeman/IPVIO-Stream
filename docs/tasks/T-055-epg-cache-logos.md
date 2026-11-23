# T-055 – Stored guide file and channel logos from the guide

Status: implemented, not built with Gradle, not device tested. JVM tests for the pure logic ran. Builds on T-053.

## Goal
1. The guide (XMLTV) was downloaded at every app start (`Xmltv.start`, no file kept), often tens of megabytes.
2. Playlists without `tvg-logo` showed no channel logos, although many guide files name an icon per channel.

## Change
- New `EpgCacheLogic.kt` (no Android): file name of the stored copy (hash of the address), reuse below 12 h, fallback up to 7 days when the download fails, removal of copies whose address is no longer used, icon address check (http/https only).
- `Epg.kt`: `fetch()` downloads into the cache folder (`cacheDir/epg_*.xml`, a `.part` file first, then renamed) or returns the stored copy; `parse()` reads from the file. `start(..., force)`: "Reload" and "Save & load" in the EPG settings and an EPG address sent from the phone pass `force = true`.
- `Epg.kt`: while reading `<channel>`, the first `<icon src>` is kept for channels of this playlist (by tvg-id or by name). `Xmltv.logo(item)`: the playlist logo first, then the icon of the guide channel, then (name match without shows) the icon of a channel with that name. A state counter makes lists already on screen draw the new logos.
- Logo call sites for channels use `Xmltv.logo(...)`: `Components.kt`, `LiveNow.kt`, `PlayerGuide.kt`, `PlayerArchive.kt`, `SportTab.kt`, `SportWatch.kt`, `Boomer.kt`. Movie and series posters are unchanged.

## Storage
New files only, in the app's cache folder (`epg_<hash>_<len>.xml`). Android may delete them when space is low; the app then downloads again. No SharedPreferences, no backup keys, no playlist cache format changed.

## Measured (outside the app)
Provider list with 2316 channels and the epg.one file: 2283 channels get a logo.

## Limits
- The guide is still parsed at each start (the shows are kept in memory only); only the download is saved.
- Icons are web images from the guide provider; they load like other logos (Coil).

## Tests
`EpgCacheLogicTest` (7 tests) and `EpgNameLogicTest` (8 tests) ran with JUnit outside Gradle: 15 of 15 passed. Gradle build: not run.

## Device test steps
1. Enter `http://epg.one/epg2.xml.gz`, "Save & load". Status "Loaded: N channels with guide". Live TV shows logos.
2. Restart the app: the guide appears without a new download (watch network or time to "Loaded").
3. "Reload": downloads again.
4. Network off, restart: the stored guide is still used.

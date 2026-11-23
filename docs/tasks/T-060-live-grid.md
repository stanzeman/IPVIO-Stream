# T-060 – Live TV as tiles

Status: implemented, not built with Gradle, not device tested. JVM tests for the pure logic ran. Builds on T-059 (logos from the guide).

## Goal (fork owner, 23 Nov 2026)
Live TV shows its categories as tiles instead of a vertical list. OK on a category shows its channels as a tile overview like Movies (logo, name, program). Search and menu entries such as the TV guide sit at the top, like in Movies.

## Decision
Additional view, switchable (Settings > Live TV > "Live TV als Kacheln", default off). The three-column list stays unchanged.

## Change
- New `LiveGridLogic.kt` (no Android): order of the category tiles (favorites, own lists, all channels, genres, favorite categories, other categories; empty ones left out), logo preview, progress, focus index after the player, initials.
- New `LiveGrid.kt`: top bar (TV guide, search, favorites, multi view, source switch with several playlists, back to list view), category grid (`LazyVerticalGrid`, adaptive columns), channel card grid, search with the app keyboard (incl. Russian layer). Uses the same channel sets, hidden categories, category order, own lists, quality bundling, channel menu, zap context and last category memory as the list view. Back: search → tiles, category → tiles (focus returns to the tile). After the player the last category opens with the last channel focused.
- `General.kt`: `liveGrid`, stored as new key `live_grid` in the existing general settings (no key renamed).
- `SettingsHub.kt`: switch in the Live TV settings. `LiveTab.kt`: shows `LiveGrid` when the switch is on.

## Not in the tile view (use the list view)
Recordings, creating/renaming own lists, moving or hiding categories, filling a list. The category tiles have no menu yet.

## Tests
`LiveGridLogicTest` (6 tests) ran with JUnit outside Gradle: 6 of 6 passed. `LiveGrid.kt` (Compose) was not compiled here; focus behaviour needs the device.

## Device test steps
1. Settings > Live TV > "Live TV als Kacheln" on. Open Live TV: top bar and category tiles; the first tile has the focus (also from the sidebar with Right).
2. OK on "Германия | Germany": channel cards with logo, show and progress. OK plays; Back from the player returns to the same card.
3. Back: category tiles, the tile you came from has the focus.
4. "Suche": keyboard left (АБВ switch), hits right. "TV-Guide", "Mehrfachansicht", "Favoriten" work. "Listenansicht" switches back.

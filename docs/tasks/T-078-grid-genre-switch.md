# T-078 – Live TV tiles: honour the switch "Genre categories in Live TV"

Status: implemented, not built with Gradle, not device tested. JVM tests ran.
Depends on: T-063 (#96, the switch), tile view of Live TV. Reported by the maintainer on 26 Nov 2026.

## Problem
In the tile view of Live TV the automatic genre categories ("Sports", "Movies", "Documentaries", "Kids", "News", "Music")
always appeared in front of the provider's categories, also with the switch "Genre categories in Live TV" off. With a
Russian provider list this looks like every category is there twice (once as a genre, once as the provider's category).
Only the list view (`LiveTab.kt`) checked the switch. Not caused by T-073 to T-077.

## Change
- `LiveGrid.kt`: the genre tiles go through `LiveGenreLogic.visible(...)` like the list view, so the switch and hidden
  categories apply; `LiveSettings.init` is called; the tiles are rebuilt when the switch changes.
- `LiveGrid.kt`: an open (or restored) genre category while the switch is off returns to the category tiles.
- Default of the switch unchanged (on).

## Tests
- `LiveGenreLogicTest`: 3/3 OK (pure Kotlin, no Gradle). `LiveGrid.kt` not compiled here.

## Manual test steps
1. Settings > Live TV: switch "Genre categories in Live TV" off.
2. Live TV in tile view: after "All channels" only the provider's categories follow, no "Sports/Movies/…" tiles.
3. Switch on again: the genre tiles come back in front of the provider's categories.
4. Open a genre tile, go to Settings, switch off, return to Live TV: the category tiles show, no empty grid.

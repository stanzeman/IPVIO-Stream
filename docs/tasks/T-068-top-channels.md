# T-068 – Most watched channels

Status: implemented, not built with Gradle, not device tested. JVM tests for the pure logic ran (13 of 13 passed).

## Goal (fork owner, 25 Nov 2026)
A row with the most watched channels at the top of the Live TV tile view, and an optional home screen row with the same channels that can be hidden like the other rows. Must not cost noticeable speed or storage.

## Decision (fork owner, 25 Nov 2026)
Ranking by watch time. A viewing counts only from 30 seconds on, so zapping does not change the list.

## Change
- New `LiveWatchLogic.kt` (no Android): watch seconds per channel id, viewings under 30 s ignored, score halves every 30 days without new viewing, at most 200 channels kept (lowest score dropped), top 10 skips channels that are no longer in the playlist, line format `id TAB score TAB ts`, running viewing (`Session`).
- New `LiveWatch.kt`: store per profile in the new SharedPreferences file `ipvio_livewatch` (key `v`). Written only when a viewing ends and lasted 30 s or more. Private categories are never recorded (same rule as the history). "Clear history" also clears it (`General.clearHistory`).
- `PlayerScreen.kt` (protected, approved by the fork owner on 25 Nov 2026): the existing 5-second tick adds 5 s while a live channel is playing (not archive, not paused, not buffering). The viewing ends on channel change, when the player closes and when the app goes to the background. No other player change.
- `LiveGrid.kt`: row "Most watched channels" (up to 10 channel cards) above the category tiles, only when watch time exists. The first card takes the focus; after the player the played card gets it back and the tiles (not a category) are shown. P+/P- in the player switch within these channels.
- `HomeLayout.kt`, `HomeScreen.kt`: new home block `topch` "Most watched channels" after "My channels". It can be switched off and moved in the home layout settings like the other blocks. It is shown only when watch time exists; adult channels follow the home screen setting.
- `DictT068.kt`, `Lang.kt`: two new UI strings in all columns (drafted, not reviewed by a native speaker).

## Cost
- Storage: one line per channel (about 60 bytes), at most 200 channels, so about 12 KB per profile.
- Speed: one write per finished viewing. The top list is sorted from at most 200 entries when the screen is built or a viewing ends.

## Not included
- Not in the backup and not in the live sync (decision of the fork owner open; adding it later means adding `ipvio_livewatch` to `Backup.NAMES`).
- Not in the Live TV list view (only tile view and home screen).
- Multi view does not count watch time.

## Tests
`LiveWatchLogicTest` (10 tests) and `DictT068Test` (3 tests) ran with JUnit 4.13.2 outside Gradle (Kotlin 2.0.21 compiler from the local Gradle install): 13 of 13 passed. Android files (`LiveWatch.kt`, `LiveGrid.kt`, `HomeScreen.kt`, `PlayerScreen.kt`) were not compiled here.

## Device test steps
1. Install, open Live TV in tile view. No new row yet.
2. Watch a channel for at least one minute, go back. The row "Most watched channels" appears above the tiles with this channel; the card has the focus.
3. Zap through a few channels for less than 30 s each and go back: the row does not change.
4. Watch a second channel longer than the first: it moves to the first place.
5. Home screen: the row "Most watched channels" appears after "My channels". Settings > home layout: switch it off, the row disappears; switch it on, it comes back; move it.
6. Settings: clear history. Both rows disappear.
7. Make a category private and watch one of its channels: it does not appear in the row.

# T-079 – Grey screen when returning to the home screen

Status: implemented, not built with Gradle, not device tested. JVM tests ran.
Depends on: T-073 (trend check at TMDB). Reported by the maintainer on 26 Nov 2026.

## Problem
After leaving a movie, a TV channel or a movie detail page, the screen stayed grey for a few seconds (most visible on
the Fire TV Stick 4K, 1st gen). Started with the stand that holds T-073 to T-077.

## Cause
`HomeScreen` (which also shows Live TV) leaves the composition while the player or a detail page is shown. When it
comes back, its `LaunchedEffect` of T-073 starts `Trending.verify` again. `verify` ran on the main dispatcher and went
through the whole playlist (filter by kind, `TrendLogic.candidates` with `MatchLogic.searchTitle` per title) before it
dropped the titles already checked today. With a large playlist this blocks the UI thread for seconds.
The kids lists (T-077) and the season merge (T-075) are built on `Dispatchers.Default` and cached; not involved.

## Change
- `Trending.kt`: `verify` remembers the key of its last finished run (`TrendLogic.verifyKey`: board day, language,
  focus, list size) and returns at once for the same key, so a return to the home screen does no work.
- `Trending.kt`: the scan over the playlist runs on `Dispatchers.Default`, also at the first start.
- A run cut short (player opened during the TMDB checks) is not marked as done and continues on the next return.
- `TrendLogic.kt`: new `verifyKey`.

## Tests
- `TrendLogicTest` (2 new) and `MatchLogicTest`: 31/31 OK with kotlinc 2.0.21 + JUnit 4, outside Gradle.
- `Trending.kt` not compiled here (Android imports).

## Manual test steps
1. Install on the living-room stick, open the home screen and wait about 30 seconds after the start.
2. Open a movie, play it, leave the player: the home screen shows at once, no grey screen.
3. Open a movie detail page and press Back: no grey screen.
4. Live TV: start a channel, leave it: the channel list shows at once.
5. "Top 10 today" still shows the same titles as before.

# T-072 – Continue watching: when titles appear, finish and expire

Status: implemented, logic tests green, not built with Gradle, not device tested.

## Goal (maintainer's wish, 25 Nov 2026)
- A short look (a few minutes) must not add a title to "Continue watching". Threshold 5 minutes instead of 30 seconds.
- A short look into another episode must not move the series card (the old exception for episode changes is dropped).
- Movies with long credits must leave the row: finished from 90 % or in the last 3 minutes (before 92 % / 90 seconds),
  still only once half has played.
- Movies leave the row after N days without watching on, series too. Both as settings, movies default 3 days,
  series default never.

## Change
- `EpisodeLogic.kt` (`ResumeLogic`): `MIN_POS` 300 s, `END_MS` 180 s, `finished` at 90 %, `shouldSave` = finished or past
  `MIN_POS` (signature kept), new `EXPIRE_DAYS` (0, 3, 7, 14, 30) and `expired(ts, now, days)`.
- `General.kt`: `resumeMovieDays` (key `resume_movie_days`, default 3) and `resumeSeriesDays` (key `resume_series_days`,
  default 0 = never). New keys in the existing `ipvio_general` file, so they are part of the backup. No existing key changed.
- `Store.kt`: `WatchStore.pruneExpired` removes expired entries (kind from the loaded playlist; unknown titles stay) and writes
  a line to the diagnostics log.
- `HomeScreen.kt`: runs `pruneExpired` when the playlist data or a setting changes.
- `SettingsHub.kt`: two select rows and a hint in Settings > Storage > History.
- `DictT072.kt` + `Lang.kt`: translations (drafted, not reviewed by native speakers).

## Effects to know
- The finished limit is shared: episode check marks and "Up next" also switch at 90 % / 3 minutes.
- An expired entry is removed completely, so the detail page no longer offers to resume that title.
- Entries are removed while the home screen is open (at start and when a setting changes), not at the exact hour.

## Tests
- `ResumeRulesT072Test`, `DictT072Test`, updated `EpisodeLogicTest` (shouldSave cases). Run with kotlinc 2.2.10 + JUnit
  (pure-Kotlin files only): 785 tests OK. Android files (`HomeScreen`, `SettingsHub`, `Store`, `General`) not compiled.
- Manual test steps:
  1. Start a movie, stop after 2 minutes: it does not show in "Continue watching". Stop after 6 minutes: it shows.
  2. Series at episode 40 in the row; play episode 42 for 2 minutes: the card still says episode 40.
  3. Play a movie to 90 % (or the last 3 minutes) and stop: it leaves the row.
  4. Settings > Storage > History: both rows show "After 3 days" (movies) and "Never" (series); change them, restart: kept.
  5. Set movies to "After 3 days" with an entry older than 3 days (or change the device date): the movie disappears on
     the home screen; Settings > Diagnostics shows "expired: …".

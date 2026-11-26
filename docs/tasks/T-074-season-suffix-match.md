# T-074 – Series names with a season ("Сезон 5") match TMDB and the trend list

Status: implemented, not built with Gradle, not device tested. JVM tests ran.
Depends on: T-073 (trend list decided by TMDB number).

## Problem
The video portal lists many series once per season, with the season in the name: "Маша и Медведь. Сезон 5.",
"Условный мент. Сезон 3", "Люпен. Сезон 2.". The TMDB search sent that whole name and found nothing; on the office
stick only 551 of about 6,000 series had a TMDB number (7,673 stored "not found"). The trend list names the series
without the season, so the name fallback failed too: "Top 10 series today" showed only 4 series.

## Change
- `MatchLogic.kt`: `searchTitle()` leaves a season at the end of the name out ("Сезон 5", "Season 2", "Staffel 1-3",
  "2 сезон", "(2-й сезон)"; Russian, English, German and a few other languages). The name is never emptied. New
  `seasonOf()` gives the season number of a name (0 = none).
- `Meta.kt`: `MATCH_V` 2 → 3, so titles stored as "not found" are searched once more with the new search title (the
  background matching retries them after the next start; up to several thousand requests once, at its usual pace).
- `TrendLogic.kt`: `rank()` takes an optional `seasonOf`; with several seasons of one series as separate playlist titles,
  the newest season is shown in the Top 10 (before: the first in playlist order).
- `HomeScreen.kt`, `Trending.kt`: trend matching uses the search title (without season).

## Risks
- A movie whose real title ends in "Season 2" or "Сезон 5" is searched without it (rare).
- Retrying all "not found" entries once costs many TMDB requests in the background after the update.
- "Невский. Расплата за справедливость. Сезон 6" becomes "Невский. Расплата за справедливость", which TMDB may still
  not find (it lists it as a season of "Невский").
- Seasons stay separate titles; merging portal seasons into one series is not part of this task (the existing season
  merge works only for Xtream series and does not know "Сезон").

## Tests
- `MatchLogicTest`: 3 new tests (season left out, titles that stay, season number).
- `TrendLogicTest`: 2 new tests (season name matches the trend title, newest season shows).
- Pure-Kotlin JUnit run without Gradle: 796 tests OK. `Meta.kt`, `Trending.kt`, `HomeScreen.kt` not compiled here.

## Manual test steps
1. Build, install, open the home screen; leave the app open some minutes (background matching retries).
2. "Top 10 series today" holds more series, e.g. "Маша и Медведь", "Условный мент" if they trend; a card opens the newest
   season ("Сезон 5").
3. Same list on both sticks.

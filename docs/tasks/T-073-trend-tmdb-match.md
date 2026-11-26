# T-073 – "Top 10 today" decides by TMDB number, not by name

Status: implemented, not built with Gradle, not device tested. JVM tests ran.
Depends on: T-064 (daily trend board), T-067 (regional focus), T-061 (language of title info).

## Problem
The "Trending · Series" list differed between devices and showed old series:
- The 2005 series "Мастер и Маргарита" took the points of the 2024 movie of the same name: the name fallback in
  `TrendLogic.rank` did not check movie vs. series.
- On one device the Russian series "Корни" (2020) was stored as "Roots" (1977): the Russian TMDB search returns both
  under the name "Корни", and `MatchLogic.pick` took the one with more votes. Another device had matched while the title
  info was German and got the right series.
- Only 7 series: the trend lists were loaded in English, so Russian playlist names ("Люпен") matched only once the
  device's background matching had found the title. Each device is at a different point of that matching.

## Change
- `MatchLogic.kt`: among hits of the same name, a hit whose original name is the title wins (after the year for movies,
  before the number of votes). New `best()` and `sameOriginal()`; `MetaRepo.rematch` uses `best()` too.
- `TrendLogic.kt`: `Board.titles` keys carry the kind (`titleKey`). `rank` takes the kind; a title with a known TMDB
  number counts only if that number is on the board, the name only while the number is unknown. New `candidates()`:
  playlist titles whose name matches a trend title of the same kind but whose number is unknown or different. The board
  stores its TMDB language (`l` in `trend2.json`); a board of another language (or one saved before this change) is not
  fresh and is loaded again once.
- `Trending.kt`: trend and discover lists are loaded in the language of the title info (`Lang.tmdb()`), same number of
  requests. New `verify()`: checks up to 40 candidates per day at TMDB right after the board is there (250 ms pause), once
  per title and day while the app runs.
- `Meta.kt`: new `MetaRepo.trendCheck()`: unknown title = normal match; matched title = one new search, switches only
  to a hit of the same name that `pick` now prefers.
- `HomeScreen.kt`: runs `verify()` after loading the board; a changed match rebuilds the lists (`Board.renewed()`).

## Risks
- Name fallback is stricter: a title with a stored wrong match is missing from the Top 10 until `verify()` has checked it
  (seconds after the home screen opens).
- Original-name preference can switch a title in a playlist that uses the translated name of a foreign title when a
  local title has that name as its original name. Playlist titles in the original language are the common case.
- `trendCheck` does not stop on HTTP 429 (MetaRepo's request helper does not report it); at most 40 searches per day.

## Tests
- `TrendLogicTest`: 4 new tests (no name fallback for a known number, kind separation, candidates, freshness per language).
- `MatchLogicTest`: 2 new tests (original name wins, year first for movies).
- Pure-Kotlin JUnit run without Gradle: 791 tests OK. `Trending.kt`, `Meta.kt`, `HomeScreen.kt` are not compiled here.

## Manual test steps
1. Build `my-build`, install on the living-room stick, open the home screen and wait about one minute.
2. "Top 10 series today" / "Trending · Series": no "Мастер и Маргарита" 2005 (unless the series itself trends), "Корни"
   with the Russian 2020 poster instead of "Roots"; close to 10 series.
3. Same on the office and bedroom sticks: the lists should match (one or two places may differ if the daily list was
   loaded hours apart).
4. Switch "Language of title info" to German and back: the list loads again each time.

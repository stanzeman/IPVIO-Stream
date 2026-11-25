# T-064 to T-066 – Top 10 today, Popular & new, New titles, Russian & Kazakh

Status: implemented, not built with Gradle, not device tested. JVM tests ran.
Depends on: T-063 (date of the latest episode of series).

## Goal
- T-064: "Top 10 today" that works for a Russian playlist: one row for movies and one for series on the home screen, and as the first row of the Movies and Series tabs. Worldwide trends with more weight on Russia and Kazakhstan.
- T-065: replace "Top rated" (old titles such as Avatar 2009) with "Popular & new".
- T-066: new lists "New titles" (above "Recently added") and "Russian & Kazakh" in the Movies and Series tabs.

## Change
- `Trending.kt`: daily board instead of a title set. Lists: `trending/movie/day` and `trending/tv/day` (3 pages each), `discover/movie` and `discover/tv` with `with_origin_country=RU` (2 pages) and `KZ` (1 page), movies of the last two years, series aired in the last 60 days. 250 ms pause per request, a 429 stops the run. Saved in `files/trend2.json` (new file, no old data touched); used up to 7 days when TMDB is not reachable. An incomplete load is shown but loaded again after 30 minutes at the earliest.
- New `TrendLogic.kt` (no Android): reciprocal-rank fusion `weight / (20 + place)`, weights world 1.0, RU 1.4, KZ 1.4; playlist titles with Russian/Kazakh original language or production country ×1.2. Match by TMDB id from the slim index, fallback by normalized title.
- New `HomeListsLogic.kt` (no Android): weighted rating `(v/(v+50))·R + (50/(v+50))·C` times a popularity factor 0.6 to 1.0 (log of the votes); "new" = newest year ≥ current year − 2; filled up with older titles below 12. Newest-first and Russian/Kazakh lists.
- `HomeScreen.kt`: `HomeData` gets `newM`, `newS`, `ruM`, `ruS` (with defaults); `topRated` removed; header buttons "Popular & new", "New titles", "Recently added", "Russian & Kazakh", "All"; home block `trend` shows two rows (layout id unchanged, so saved home layouts stay).
- `HomeLayout.kt`: labels of the blocks `topm` / `tops` (ids unchanged).
- `DictT064.kt` + `Lang.kt`: translations of the new texts (also "Completing series data" from T-063). Drafted, not reviewed by native speakers.

## Risks
- The rows fill as the background detection finds TMDB ids; right after a new playlist they can be short.
- Without a TMDB key there are no trends (as before).
- About 12 TMDB requests per day and device.

## Tests
`TrendLogicTest` (7), `HomeListsLogicTest` (6), `DictT064Test` (3). Ran with the Kotlin compiler and JUnit outside Gradle.

## Manual test steps
1. Home screen: "Top 10 movies today" and "Top 10 series today" show up with numbers, Russian titles among them.
2. Movies tab: first row "Top 10 today"; buttons at the top "Popular & new", "New titles", "Recently added", "Russian & Kazakh", "All movies".
3. "New titles" in Series: a series with a new season this year is near the top.
4. "Russian & Kazakh": only Russian/Kazakh productions, newest first.
5. Airplane mode, restart: the Top 10 rows still show the last result.

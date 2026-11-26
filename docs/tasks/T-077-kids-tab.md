# T-077 – "Kids" area: cartoons and anime

Status: implemented, not built with Gradle, not device tested. JVM tests ran.
Depends on: T-076 (cartoon/anime recognition), T-064 (top lists), T-075 (combined seasons). Asked for by the maintainer on 26 Nov 2026.

## Goal
A new menu entry "Kids" right under "Series", built like the Movies/Series tabs: big picture, top rows, personal rows and
tabs at the top. It holds everything animated; anime is kept apart from other cartoons.

## Change
- `KidsLogic.kt` (new, pure Kotlin): `classify(genreIds, lang, group)` gives none / cartoon / anime. Anime = provider
  category named for anime ("Аниме", "Anime", …) or TMDB genre 16 with Japanese original language; every other title that
  `TrendLogic.isAnimation` accepts is a cartoon. `top()` merges trend titles, "Popular & new" and newest added (each once).
  `heroMix()` picks the big-picture titles from the four top rows in turn.
- `MenuLogic.kt`: area `KIDS = 12`, `COUNT = 13`, default order `0, 3, 1, 2, 12, …`. A saved menu order gets the new
  area right after "Series" (not before "Settings" like other new areas). Can be hidden in Settings > Menu.
- `Store.kt`: menu label "Kinder" (English "Kids", row already in the dictionaries). `Sidebar.kt`: icon 12 (smiling face).
- `MainActivity.kt`: 13 list states, so `states[tab]` exists for the new area.
- `HomeScreen.kt`: `buildKids()` in `buildHome` (cartoon movies, cartoon series, anime movies, anime series; never adult
  titles; the T-076 option does not apply here). New branch for the area: big picture, rows "Top animated movies",
  "Top animated series", "Top anime movies", "Top anime series", "Continue watching", "My list", "Recently watched"
  (kids titles only), "Newly added". Tabs at the top: "All movies", "All series", "All anime", "Newly added".
- `DictEn1.kt`: English texts for the new rows and pages.

## Acceptance criteria
- The menu shows "Kids" under "Series", also for users with a saved menu order.
- Cartoons (TMDB "Animation" or a cartoon category such as "мультфильмы") are in the cartoon rows, anime in the anime rows.
- No adult title appears in the area.
- Without TMDB data the rows still fill from the provider categories (newest added).

## Risks
- Anime is not always for children (all anime is included, as asked). An age filter can be a later option.
- A Japanese animated film in a "мультфильмы" category counts as anime.
- Daily TMDB trends hold few animated titles; the top rows are mostly "Popular & new" (TMDB rating and year).
- `buildHome` does a little more work (one classification per movie/series).

## Tests
- `KidsLogicTest` (4 new tests); menu tests in `CoreLogicTest` and `Round17Test` updated (+1 test for the order).
- Pure-Kotlin JUnit run without Gradle: 805 tests OK. `CoreLogicTest` is not runnable here (Android references); its menu
  tests were run separately with `MenuLogic.kt`: 6/6 OK. `HomeScreen.kt`, `Sidebar.kt`, `MainActivity.kt`, `Store.kt` not compiled here.

## Manual test steps
1. Menu: "Kids" (German "Kinder") sits under "Series" and shows a smiling face icon.
2. Open it: big picture with cartoons/anime, rows "Top animated movies/series", "Top anime movies/series" (e.g. "Маша и Медведь"
   in the cartoon series, Japanese anime in the anime rows).
3. Tabs at the top: "All movies", "All series", "All anime", "Newly added" open grids with only those titles.
4. Watch a cartoon for 5 minutes: it shows in "Continue watching" in the Kids area.
5. Back from the rows goes to the tabs at the top, then to the menu (like Movies/Series).
6. Settings > Menu: "Kids" can be hidden and moved.

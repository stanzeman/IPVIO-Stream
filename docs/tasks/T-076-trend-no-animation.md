# T-076 – Option: no cartoons and anime in the top lists and the big picture

Status: implemented, not built with Gradle, not device tested. JVM tests ran.
Depends on: T-064 (top lists), T-073 (trend matching). Asked for by the maintainer on 26 Nov 2026.

## Goal
A setting that keeps animated movies, animated series and anime out of "Top 10 today", "Trending" and the big picture
(slideshow) of the home screen and the Movies/Series tabs. Default on.

## Change
- `TrendLogic.kt`: `isAnimation(genreIds, group)`: TMDB genre 16 ("Animation", anime included) or a provider category
  whose name contains a cartoon/anime word ("мульт", "аниме", "anime", "animation", "cartoon", "zeichentrick", …), so
  titles without TMDB data are caught too (the video portal has "мультфильмы", "мультсериалы", "Аниме").
- `HomeScreen.kt`: with the option on, such titles are left out before ranking the top lists (the list fills up with the
  next titles) and out of the newest titles of the big picture. The option is part of the home cache key.
- `Store.kt`: `AppSettings.trendNoAnimation`, new key `trend_no_animation` (default true, as the maintainer asked) in the existing settings file.
- `HomeLayout.kt`: switch on Settings > "Customize home" (German key "Startseite anpassen"); `DictEn1.kt`: English text.

## Risks
- A provider category whose name merely contains one of the words (e.g. "мульт…" in another sense) is treated as cartoons.
- Live-action family films with TMDB genre 16 are rare but would be left out.

## Tests
- `TrendLogicTest`: 1 new test (genre, provider categories, normal categories).
- Pure-Kotlin JUnit run without Gradle: 801 tests OK. `HomeScreen.kt`, `Store.kt`, `HomeLayout.kt` not compiled here.

## Manual test steps
1. Settings > Customize home: "No cartoons and anime in the Top 10 and big picture" is on (default).
2. Home, Movies, Series: the top rows and the big picture show no "Маша и Медведь", no anime; the lists still have 10 titles
   if enough others trend.
3. Categories "мультфильмы" and search still show them. Switch off: they return.

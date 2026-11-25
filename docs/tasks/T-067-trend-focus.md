# T-067 – Regional focus of "Top 10 today" follows the language of the title info

Status: implemented, not built with Gradle, not device tested. JVM tests ran.
Depends on: T-061 (setting "Language of title info"), T-064 (daily trend board).

## Goal
The extra weight on Russia and Kazakhstan is optional: on only with Russian title info. Other Russian-speaking / former Soviet countries count too, with less weight than Russia and Kazakhstan.

## Change
- `TrendLogic.kt`: `focusFor(metaCode)` ("ru" = focus, else none); `W_CIS = 1.15` for UA, BY, UZ, KG, TJ, TM, AZ, AM, GE, MD, LT, LV, EE (one `discover` request each for movies and series, countries joined with "|"); playlist factor `boost()`: 1.2 RU/KZ, 1.1 other former Soviet countries or languages, 1.0 otherwise or without focus. The board stores its focus (`f` in `trend2.json`); a board of another focus is not "fresh" and is loaded again at once.
- `Trending.kt`: region lists only with the focus (without it: 6 requests per day instead of about 14).
- `HomeScreen.kt`: boost per title from the board's focus; the trend list is asked again when the language of the title info changes.
- The "Russian & Kazakh" row is unchanged.

## Follow-up: original language instead of country
"Rise of the Conqueror" (Uzbek-American, English) came first only because of its production countries. Now:
- `discover` lists by `with_original_language`: `ru` (2 pages, 1.4), `kk` (1 page, 1.4), and `uk`, `be`, `uz`, `ka`, `hy`, `az`, `ky` one by one (1 page, 1.15). No combined "|" query, because it is not certain that TMDB accepts it for this parameter. About 26 requests per day with the focus (with 250 ms pause, 429 stops).
- Playlist factor: 1.2 Russian/Kazakh original language, 1.1 another former Soviet language, 1.05 only a production country from there, else 1.0.
- Focus id is now "ru2", so a board saved today with the country lists is loaded again at once.

## Tests
`TrendLogicTest`: 5 new tests (focus, boost by original language, region lists, no boost without focus, freshness per focus).

## Manual test steps
1. Language of title info = Russian: "Top 10 today" contains Russian/Kazakh titles near the top.
2. Switch to German, back to the home screen: the list is loaded again and shows the worldwide trends only.
3. Switch back to Russian: the focus is back.

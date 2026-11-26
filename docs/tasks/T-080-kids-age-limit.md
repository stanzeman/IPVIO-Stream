# T-080 – Age limit for the "Kids" area

Status: implemented, not built with Gradle, not device tested. JVM tests ran.
Depends on: T-077 (Kids area). Asked for by the maintainer on 26 Nov 2026 ("South Park" showed up in the Kids area).

## Goal
A setting with an age limit for the Kids area. When a title has several ratings, the highest counts.

## Change
- `AgeRatingLogic.kt`: `minAge()` reads a certification as an age (numbers, "FSK 16", "16+", "12A", US movie and TV
  ratings: G/U 0, PG 6, PG-13 12, R/NC-17 18, TV-Y/TV-G 0, TV-Y7 6, TV-PG 12, TV-14 14, TV-MA 18). `highest()` picks the
  highest of several ratings of one country. `strictest()` = the stricter of the app country's rating and the US rating.
  `movieCert()` now takes the highest of all releases (before: the theatrical one).
- `Meta.kt`: ratings per country keep the highest entry (before the last entry won, e.g. one of several season ratings).
  New field `ageMin` (cache key `am`, in the slim index and the single files; old entries read as "not asked").
  `fetchKidsAge()` loads only `content_ratings` / `release_dates` of a title with a known TMDB number. The shown age label
  on the detail page is unchanged (own country, US fallback), now from the highest entry.
- `Parental.kt`: `kidsAge` (key `parental_kids_age`, default 12, -1 = off) and `kidsHideUnrated` (key
  `parental_kids_unrated`, default false) in the existing per-profile settings file `ipvio_general`.
- `SettingsHub.kt`: Settings > Parental control, section "Kids tab": "Age limit in the Kids tab" (Off, 0, 6, 12, 16) and
  "Hide titles without an age rating". Behind the parental PIN like the rest of the page.
- `HomeScreen.kt`: `buildKids()` filters by `KidsLogic.allowed()`; missing ratings of kids titles are loaded in the
  background (one request each, 250 ms pause, the lists rebuild every 150 titles and at the end; stops after 5 failures
  in a row). The setting and the load progress are part of the home cache key.
- `KidsLogic.kt`: `allowed(ageMin, limit, hideUnrated)`.
- `DictEn1.kt`: English texts.

## Risks
- Cache format: one new optional key (`am`); older app versions ignore it. No migration needed.
- Many kids titles: the first load takes a while (about 4 titles per second); until then unknown titles are shown
  (unless "Hide titles without an age rating" is on).
- US ratings on the strict side: many western cartoons rated TV-PG count as 12.
- A title without a TMDB match never gets a rating (shown, or hidden with the second switch).

## Tests
- `AgeRatingLogicTest`: 3 new tests, the movie test now expects the highest release. `KidsLogicTest`: 1 new test.
- Pure-Kotlin JUnit run without Gradle: 811 tests OK. `Meta.kt`, `HomeScreen.kt`, `SettingsHub.kt`, `Parental.kt` not compiled here.

## Manual test steps
1. Settings > Parental control: section "Kids tab", "Age limit in the Kids tab" = 12 (default).
2. Open the Kids tab, wait one or two minutes: "South Park" (TV-MA) and similar titles disappear; the rows fill with others.
3. Set the limit to 6: fewer titles; set "Off": all cartoons and anime again.
4. Switch on "Hide titles without an age rating": titles without TMDB data leave the tab.
5. With a PIN set, the page asks for it before the limit can be changed.
6. Detail page of a series with several ratings shows the highest rating of your country.

# T-075 – "Combine seasons" for the video portal, on by default

Status: implemented, not built with Gradle, not device tested. JVM tests ran.
Depends on: T-065 (#151, "Combine seasons" for Xtream), T-051 (video portal), T-074 (season left out of the TMDB search).
Approved by the maintainer on 26 Nov 2026, including "on by default".

## Goal
The video portal lists many series once per season ("Маша и Медведь. Сезон 5.", "Люпен. Сезон 2."). A series should
appear once, with all its seasons on the detail page, so it also matches TMDB and the "Top 10 today" list.

## Change
- `SeasonMergeLogic.kt`: the marker also accepts Russian "Сезон N" (any case), a range "Сезон 2-3" (first season), "2 сезон",
  "2-й сезон" and a dot at the end. New `mergeable(url)`: Xtream placeholder (`.xtser`) or portal placeholder (`portal://…/series/N.ser`).
- `Models.kt` (`M3uParser.parseLines`): with the option on, portal placeholders are combined like Xtream ones.
- `PortalLogic.kt`: new `lazyParts()` returns every placeholder with its season (before: only series with exactly one
  placeholder counted); `lazyUrls()` builds on it.
- `LazySeries.kt`: portal series load every placeholder and move episodes numbered from season 1 into the season of the
  placeholder (`SeasonMergeLogic.remap`, as for Xtream). Stalker unchanged.
- `Store.kt`: `mergeSeasons` default `true` (key `merge_seasons` unchanged; a value the user set stays).
- `SettingsHub.kt`, `DictEn1.kt`: the option text names video portals and "Сезон 5" (German key and English text changed together).

## Effect on user data
As in T-065: a combined series has the id `s_<title without season>`. History, resume positions and "My list" of the
single-season entries are not shown for the combined series; they are not deleted and come back when the option is
switched off. With the default on this applies to every device after the update (also to Xtream playlists with
"Show S01" style series), unless the option was switched off before.

## Risks
- "Люпен" (no season in the name) and "Люпен. Сезон 1." both count as season 1 and become two sources of it. If the entry
  without a season holds all seasons, its episodes (portal numbers them as season 1) mix into season 1.
- "Сезон 2-3" is placed as season 2.
- Opening a combined series sends one portal request per placeholder (e.g. 6 for "Маша и Медведь").
- A real title ending in "N сезон" or "Сезон N" is split when another entry of the same base exists.

## Tests
- `SeasonMergeLogicTest`: 2 new tests (Russian portal names, mergeable placeholders).
- `SeasonMergeParseTest`: 2 new tests (portal off: unchanged; portal on: combined, each placeholder with its season).
- Pure-Kotlin JUnit run without Gradle: 800 tests OK. `LazySeries.kt`, `Store.kt`, `SettingsHub.kt` not compiled here.

## Manual test steps
1. Build, install. Settings > Playlists > Series: "Combine seasons" is on (unless switched off before).
2. Series: "Маша и Медведь", "Люпен", "Условный мент" appear once; the detail page lists seasons 1, 2, … and plays episodes.
3. "Top 10 series today" holds these series once (after TMDB matching).
4. Switch the option off: the single-season entries and their history come back.

# T-063 – Date of the latest episode of series

Status: implemented, not built with Gradle, not device tested. JVM tests ran.

## Goal
"New titles" and "Popular & new" shall sort series by their latest episode, not by their first year (a series from 2019 with a new season in 2026 is new).

## Change
- `Meta`: new fields `lastAir` (date) and `lastAirTs` (when asked). Stored in the slim index (`la`, `lat`) and in the single files; older files simply have none.
- `MetaRepo.fillLastAir`: one TMDB request (`/tv/{id}`) per series; also fills a missing country. The detail page sets it too (the details already contain it).
- `Classifier`: new phase after the country phase (internal name `"Serien"`): series with a TMDB hit for which `SeriesAirLogic.due` is true; same pace as the country phase (12 at once, 150 ms pause, paused while playing, stops on low memory).
- `MetaBadge`: shows "Completing series data" during that phase.
- New `SeriesAirLogic.kt` (no Android): refresh rule (never asked, 7 days for series with an episode in the last two years, otherwise 90 days), sort key "newest first".

## Load on TMDB
One request per found series at the first run (a few thousand for a big list, spread over minutes), then only due ones.

## Tests
`SeriesAirLogicTest` (6 tests).

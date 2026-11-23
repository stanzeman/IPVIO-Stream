# T-053 – Guide by channel name (no tvg-id)

Status: implemented, not built with Gradle, not device tested. JVM tests for the pure logic ran.

## Goal
A playlist whose channels have no `tvg-id` (and no `url-tvg` in its head) showed no TV guide, even when the provider names an XMLTV address. `Xmltv.start` stopped with "no channel ids" because it only matched by `tvg-id`.

## Change
- New `EpgNameLogic.kt` (no Android): `key(name)` reduces a channel name (lower case, "ё" as "е", letters, digits and "+" only, quality words uhd/fhd/hd/sd/4k removed); `pick(ids, shows)` takes the guide channel with the most shows (ties: smaller id).
- `Epg.kt` (`Xmltv`): for live channels without `tvg-id` the name keys are collected; while the XMLTV file is read, `<channel>` entries whose `display-name` has such a key are remembered, and their shows are read too. After loading, each channel title maps to one guide id (`byTitle`). `idFor(item)`, `has(item)`, `forItem(item)` use the `tvg-id` first, then the name match.
- Call sites that asked `Xmltv.forChannel(item.tvgId)` now use `Xmltv.forItem(item)` (`Epg.kt`, `EpgFull.kt`, `GuideSearch.kt`, `RemoteHub.kt`, `SportTab.kt`, `SportHub.kt`).
- Channels with a `tvg-id` are matched exactly as before.

## Measured (outside the app, same key function)
Provider list with 2316 live channels against the simplified XMLTV file of epg.one (3156 channels, 22 to 23 Nov 2026): 2283 names found, 1966 channels with at least one show. Without shows in the file: mostly sports (123), movies (77), German channels (47). Time-shifted channels ("+2", "+4") have their own names; reading them from the base channel with an offset is not part of this task (3 more channels only).

## Limits
- The past days kept for catch-up (`past` map in `Xmltv.start`) still use the `tvg-id` only; channels matched by name have no past shows.
- Names must match after reduction. A guide with other channel names finds nothing.
- A list without `tvg-id` that names an XMLTV address in its head now loads that file too (before it was skipped). Memory use on weak devices is unchanged for lists with ids; for such lists it is new.
- Playlist cache format is unchanged (the id is not stored).

## Files
`EpgNameLogic.kt`, `Epg.kt`, `EpgFull.kt`, `GuideSearch.kt`, `RemoteHub.kt`, `SportTab.kt`, `SportHub.kt`, `EpgNameLogicTest.kt`, `CHANGELOG.md`.

## Tests
- `EpgNameLogicTest` (8 tests) ran with JUnit outside Gradle: 8 of 8 passed. Gradle build and `Epg.kt` parsing: not run.

## Device test steps
1. Load a playlist without `tvg-id` (Russian provider list). Settings > Live TV (EPG): enter an XMLTV address, for example `http://epg.one/epg2.xml.gz`, then "Save & load".
2. After loading, the status reads "Loaded: N channels with guide". Open Live TV: channels show the current show; the channel guide lists the following shows.
3. Check a channel with a `tvg-id` playlist: unchanged.
4. On a weak device: watch memory; if the app stops reading ("shortened, memory low"), use `http://epg.one/epg2.xml` (current day only, no descriptions).

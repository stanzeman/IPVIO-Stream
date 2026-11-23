# T-059 – Stored guide, background refresh every 12 hours

Status: implemented, not built with Gradle, not device tested. JVM tests for the pure logic ran. Builds on T-055.

## Goal (fork owner, 23 Nov 2026)
The guide shall stay stored for 36 hours and be read again every 12 hours in the background, without shows and channel icons disappearing from Live TV in between.

## Before
- At every start the XMLTV file was read again (T-055 saved only the download); until then Live TV had no shows and no guide logos.
- A running app never read the guide again (shows reach 36 hours ahead, so after 1.5 days the guide ran out).
- `Xmltv.start` cleared its maps and filled them again; a failed read left an empty guide.

## Change
- New `EpgSnapshot.kt` (no Android): `EpgData` (shows, name matches, icons), file format of the stored copy (gzip, magic, version), signature of what it was read for (addresses, channel ids, name keys, catch-up days), rules `usable` (same signature, younger than 36 h), `refreshDue` (12 h), `replace` (a read without shows never replaces a shown guide of the same channels).
- `Epg.kt` (`Xmltv`): one `@Volatile` `EpgData` replaced in one step after a complete read. Start: the stored copy (`files/epg_snapshot.bin`) is shown at once if usable; it is read again only when due, when the channels changed, or with `force`. While the app runs a check every 30 minutes reads the guide again when it is 12 hours old (download forced). After a successful read the copy is stored (temporary file, then renamed).

## Storage
New file `epg_snapshot.bin` in the app's files folder. No SharedPreferences key, backup key or playlist cache changed. Global, not per profile (like the EPG address).

## Limits
- After a channel list change the guide is read from the stored XMLTV file (download only if that is older than 12 hours).
- The stored copy holds only the shows of the channels of the playlist at that time.

## Tests
`EpgSnapshotTest` (6 tests: round trip, damaged file, 36 h, 12 h, no wipe on failed read, signature) ran with JUnit outside Gradle: 6 of 6 passed. `Epg.kt` was not compiled here.

## Device test steps
1. Load the guide ("Save & load"). Restart the app: shows and logos are there at once (status "Loaded: N channels with guide" without "Wird geladen").
2. "Reload": Live TV keeps its shows and logos while the new guide is read.
3. Network off, "Reload": status says the update failed, the old guide stays.
4. Leave the app running more than 12 hours: the guide is read again in the background (status time changes), no gap.

## Follow-up: guide logos in all channel views (23 Nov 2026)
The Live TV channel rows still drew the playlist logo only and showed initials ("MO", "PR"). Now these also use `Xmltv.logo(item)`: channel rows (`LiveTab.kt`), player info bar, overlay and channel list (`PlayerLive.kt`), channel tiles on the home screen (`HomeParts.kt`), the large card fallback (`Components.kt`). For movies and series `Xmltv.logo` returns the playlist logo unchanged.

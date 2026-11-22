# T-037 – Categories from #EXTGRP lines

Status: implemented, not built with Gradle, not device tested.

## Goal
A provider list that gives each channel its category only as `#EXTGRP:name` (no `group-title` attribute) showed every channel under "Other" (shown as "Sonstige" in the app).

## Cause
`Models.kt` (`entries`) read the category only from `group-title`. Lines starting with `#` other than `#EXTINF` were skipped, so `#EXTGRP` was ignored.

## Change
- `LoadLogic.extGroup(line)` reads a `#EXTGRP:name` line; `LoadLogic.pickGroup(groupTitle, extGrp)` picks group-title first, then `#EXTGRP`, else "Sonstige".
- `Models.kt` `entries`: remembers an `#EXTGRP` value that comes before or after its `#EXTINF` line, resets both after each address.
- Xtream, Stalker and portal lists write `group-title`, so they are not affected.

## Affected files
`LoadLogic.kt`, `Models.kt`, `LoadLogicTest.kt`, `CHANGELOG.md`.

## Risks
Lists that carry both: group-title wins, so nothing changes for them. A list with a stray `#EXTGRP` line before an entry without any group now uses that group (the VLC convention).

## Tests
- `LoadLogicTest`: 3 new tests (extGroup, pickGroup, parser with #EXTGRP after / before #EXTINF, group-title priority, fallback). Run with JUnit outside Gradle: 16 of 16 passed. Gradle build: not run.

## Device test steps
1. Load a list whose entries use `#EXTGRP:` only. Live TV > categories shows the provider's categories, not one "Other".
2. "Reload playlist completely" once (the raw list is cached for 12 h).
3. Check a Stalker/Xtream list: categories unchanged.

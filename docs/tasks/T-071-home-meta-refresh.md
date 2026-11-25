# T-071 – Home rows without TMDB data after an app start

Status: implemented, not built with Gradle, not device tested.

## Problem
Reported on 25 Nov 2026: the "Russian & Kazakh" rows (Movies/Series tabs and the optional home rows from T-069) were empty.
Switching "Automatic genres" on and off made them appear.

Cause: `HomeScreen` builds its data (`buildHome`) right after the start. At that moment `MetaRepo.loadIndex` has not run yet,
so `MetaRepo.cached` returns null for every title. `Classifier.run` loads the index but raises `Classifier.version` only
when titles are new (`todo`), countries are missing (`need`) or series data is due. On a library without changes none of
that happens, the home data is never rebuilt and every list that needs metadata stays empty. Toggling the setting changed a
key of `HomeCache` and forced a rebuild.

## Change
- `Meta.kt`: `MetaRepo.isIndexLoaded()` (read-only view of the existing flag).
- `Store.kt`: `Classifier.run` raises `version` once after the first index load of the process. The home screen adopts the
  version within 45 s (existing poll), so the rows fill shortly after the start.

## Risks
One extra rebuild of the home data per app start (background thread). No storage change.

## Tests
No JVM test: the change is in Android-bound code (`Classifier` holds Compose state, `MetaRepo` reads app files).
Manual test steps:
1. Force-stop the app, start it again, wait about a minute on the Movies tab.
2. The "Russian & Kazakh" row appears without touching any setting; "Popular & new" is filled too.
3. Home: the switched-on "Russian & Kazakh · Movies/Series" rows appear.

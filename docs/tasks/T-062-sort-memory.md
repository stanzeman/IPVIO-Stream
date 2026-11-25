# T-062 – Categories remember their sort

Status: implemented, not built with Gradle, not device tested. JVM tests ran.

## Goal (fork owner, 25 Nov 2026)
A category opens with "Newly added". After switching to "Popularity" and leaving, the next visit shall open with "Popularity" again.

## Change
- New `BrowseSortLogic.kt` (no Android): start sort (remembered > fixed default from the settings > requested), only category views (requested sort = "Newly added") remember.
- New `BrowseSortPrefs.kt`: per profile and type, new preference file `ipvio_browsesort` (no existing key changed).
- `BrowseScreen.kt`: start sort from `BrowseSortLogic.initial`; picking a sort in a category view stores it; "Reset" forgets it.

## Tests
`BrowseSortLogicTest` (5 tests).

## Device test steps
1. Movies > a category > sort "Popularity". Back, open another movie category: "Popularity".
2. Series categories are not affected. "Reset" goes back to "Recently added" and forgets it.

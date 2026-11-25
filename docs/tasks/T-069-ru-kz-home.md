# T-069 – "Russian & Kazakh": no header button, optional home rows

Status: implemented, not built with Gradle, not device tested. JVM tests ran.
Depends on: T-064 to T-066 (lists `ruM` / `ruS`).

## Problem
On the Movies and Series tabs the header holds six tabs since T-064 ("Categories", "Popular & new", "New titles",
"Recently added", "Russian & Kazakh", "All movies/series"), next to the search pill, playlist pill, clock, bell and profile.
The design is 1658 px wide; the tab row did not fit and its container clips (`clipToBounds`), so the last tabs,
"Russian & Kazakh" and "All …", got no width and could not be seen or focused.

## Decision (maintainer, 25 Nov 2026)
No "Russian & Kazakh" button in the header; only the rows.

## Change
- `HomeScreen.kt`: the header tab "Russian & Kazakh" is removed.
- `Header.kt`: the tab row scrolls sideways (`horizontalScroll`). A focused tab brings itself into view, so Right on the
  remote reaches every tab. Labels unchanged.
- `HomeLayout.kt`: two new home blocks `rum` ("Russian & Kazakh · Movies") and `rus` ("Russian & Kazakh · Series"),
  hidden by default, switched on in "Customize home". New blocks are appended at the end of a saved order.
- `BlockLayoutLogic.kt` (new, no Android): a block that a saved layout does not know yet starts hidden if it is hidden by
  default. Before, any new block appeared at once on a home screen the user had arranged. No pref key renamed; the stored
  format is unchanged.
- `HomeScreen.kt`: home blocks `rum` / `rus` show the same lists as the tabs (at least 3 titles, first 40, "Show all" opens the full list).
- `DictT064.kt`: descriptions of the two blocks (all columns of this dictionary; drafted, not reviewed by native speakers).

## Not changed
The "Russian & Kazakh" row inside the Movies and Series tabs: the code is intact (below "Recently added", needs 3 titles with
Russian/Kazakh original language or production country from TMDB). To be checked on the device.

## Risks
- Scrolling header: the first tab keeps its focus requester; Up from the first row still goes to the profile circle.
- Users with a saved layout who had hidden nothing: unaffected except that the two new blocks start hidden (intended).

## Tests
`BlockLayoutLogicTest` (3), `DictT064Test` (extended). Ran with the Kotlin compiler 2.0.21 and JUnit 4.13.2 outside Gradle: 6 tests passed.

## Manual test steps
1. Movies tab: the tabs at the top are "Categories", "Popular & new", "New titles", "Recently added", "All movies"; move Right, the row moves along and "All movies" can be opened.
2. Same in the Series tab.
3. Settings > Customize home: "Russian & Kazakh · Movies" and "· Series" are listed as hidden at the end; switch on, move up.
4. Home screen: both rows appear with Russian/Kazakh titles; "Show all" opens the full list.
5. Rows you arranged earlier stay as they were.

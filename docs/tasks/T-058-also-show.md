# T-058 – Switch "shown additionally" on again (fork only)

Status: implemented, not built with Gradle, not device tested. Fork only: not for a pull request to upstream, because upstream switched this off on purpose (code comment: "Show as well can no longer be switched on (1.4.2)"; the reason is not documented).

## Goal
The fork owner uses a live TV M3U as main list and a video portal for movies and series. Both must be shown at the same time.

## Change
- `PlaylistEditDialog.kt`: the row "Zusätzlich angezeigt" is shown for every list that is not the main list (before: only when it was already shown, to switch it off). "Apply" stores on and off (`PlaylistStore.setShown(ctx, index, shown)`), then `applyMulti` loads the extra list as before.
- `PlaylistEditDialog.kt`: the window is limited to the screen height and only its middle part scrolls (`weight(1f, fill = false)` instead of a fixed 420 dp). With the extra row the window was taller than a 1080p TV screen (about 540 dp) and the buttons "Apply"/"Cancel" were cut off (reported on the Fire TV, 23 Nov 2026).
- Merging, source filter and Live TV source switch are unchanged upstream code (`MultiLogic`, `PlaylistStore.applyMulti`).

## Risks
- Memory: each extra list needs memory (the app has its own check "Not enough memory for another playlist on this device").
- Upstream removed the switch for a reason that is not written down; problems that led to it may come back.

## Tests
`PlaylistManageLogicTest`: new test for switching on and off as a change. Ran with JUnit outside Gradle. The dialog itself needs a device.

## Device test steps
1. Make the live TV M3U the main list. Settings > Playlists > the portal list > Edit: "Zusätzlich angezeigt" = On, "Übernehmen".
2. Movies and Series show the portal titles; Live TV still shows the M3U channels.
3. Switch it off again: the portal titles disappear.

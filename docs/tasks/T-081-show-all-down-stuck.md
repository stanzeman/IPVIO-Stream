# T-081 – Focus stuck on "Show all" when pressing Down

Status: implemented, not built with Gradle, not device tested.
Reported by the maintainer on 26 Nov 2026.

## Problem
Scrolling down on the home screen: when the highlight lands on "Show all" of a row, Down does nothing and the
highlight stays there.

## Cause
`RowTitle` (`HomeParts.kt`) handles Down itself and requests focus on the first card of the row. If the row is
shifted sideways, the first card is not composed and the request fails. The fallback (scroll the row to the start,
retry) only ran when `requestFocus()` threw. Current Compose does not throw for a requester without a target; it
returns `false`. So the fallback never ran, the key was consumed, and the focus stayed on "Show all".
Same pattern in the search rows (`SearchScreen.kt`, "Show all" → first card) and in the sport hub pages
(`SportUi.kt`, first focus on open, fallback to "Back").

## Change
- `Nav.kt`: new `FocusRequester.tryFocus()`, true only if the request worked (no exception and result true).
- `HomeParts.kt` and `SearchScreen.kt`: use `tryFocus()`; after scrolling to the start and retrying, fall back to
  the normal Down move so the focus can never get stuck.
- `SportUi.kt`: use `tryFocus()` so the fallback to "Back" also runs when the request returns false.

## Tests
- No JVM test: focus handling needs Compose on a device (section 17 of CLAUDE.md).
- Not compiled here (Android imports).

## Manual test steps
1. Home screen: in a row, move a few cards to the right, press Down to the next row, then Up to its "Show all",
   then Down: the first card of that row is highlighted (the row scrolls to its start).
2. Keep pressing Down through all rows of the home screen, also in the Movies, Series and Kids tabs: never stuck.
3. Search: type a term, move right in a result row, Up to "Show all", Down: the first card is highlighted.
4. Sports hub: open a page; the first entry (or "Back") is highlighted.

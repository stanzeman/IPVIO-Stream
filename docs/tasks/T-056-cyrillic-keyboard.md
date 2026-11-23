# T-056 – Russian layer on the search keyboard

Status: implemented, not built with Gradle, not device tested. JVM tests ran.

## Goal
Titles and channels with Cyrillic names could only be searched with the device keyboard. The app's own search keyboard had Latin letters, symbols and Western/Slavic special letters, but no Cyrillic.

## Change
- New `KeyboardLogic.kt` (no Android): the letter rows (Latin, Russian ЙЦУКЕН with all 33 letters, symbols, special letters), the row choice and the label of the switch key. The current alphabet is kept while the app runs.
- `SearchScreen.kt` (`SearchKeyboard`): rows come from `KeyboardLogic`; a new key "АБВ"/"ABC" in the bottom row switches the alphabet. Layout of the other keys unchanged; the special-letter key stays.

## Risks
The bottom row gets one more key, so the other keys there are a little narrower. Row 1 and 2 of the Russian layer have 11 keys (Latin: 10 and 9).

## Tests
`KeyboardLogicTest` (4 tests) ran with JUnit outside Gradle: 4 of 4 passed. Gradle build: not run.

## Device test steps
1. Open search. Bottom row: "АБВ". Press it: the rows show й ц у к е н …
2. Type "мистер": hits appear like with Latin letters.
3. "ABC" switches back. Leave search and open it again: the last alphabet is still selected.

# T-061 – Language of the title information (TMDB)

Status: implemented, not built with Gradle, not device tested. JVM tests for the pure logic ran.

## Goal (fork owner, 23 Nov 2026)
Show TMDB data (descriptions, titles, cast) in Russian while the menus stay German. The app has no Russian UI language, and TMDB requests used the app language (unknown languages fell back to German).

## Change
- New `MetaLangLogic.kt` (no Android): choices ("" = like the app, ru, de, en, uk and the other app languages), effective code, TMDB tag (adds ru-RU and uk-UA; the existing tags are unchanged).
- `General.kt`: `metaLang`, new key `meta_lang` in the general settings.
- `Lang.kt`: `metaCode()`; `tmdb()` uses it.
- `Meta.kt`: every language check and request uses `Lang.metaCode()`. Stored details carry their language (`uiLang`), so a title loaded in another language is fetched again when it is opened (existing rule in `MetaFreshLogic`). Age ratings keep the country of the app language (two places).
- `Credits.kt`: cache keys use `Lang.metaCode()`.
- `SettingsHub.kt` (Language & region): choice "Sprache der Titel-Infos" with a hint.

## Not changed
Genres are TMDB genre ids and do not depend on the language; the background genre pass does not run again. Menus, dictionaries and the app language are unchanged.

## Tests
`MetaLangLogicTest` (5 tests) ran with JUnit outside Gradle: 5 of 5 passed. Gradle build: not run.

## Device test steps
1. Settings > Language & region > "Sprache der Titel-Infos" = Русский.
2. Open a movie: description, cast and title infos in Russian; menus stay German. Age rating still German (FSK).
3. Back to "Wie App-Sprache": opening the movie again shows German texts.

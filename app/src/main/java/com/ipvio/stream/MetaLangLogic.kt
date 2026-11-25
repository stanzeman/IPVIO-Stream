package com.ipvio.stream

/**
 * Language of the title information from TMDB (descriptions, titles, cast, episode names, trailer language), separate
 * from the language of the app (T-061). Without Android, testable.
 *
 * The app's menus exist in 12 languages; TMDB has many more. Someone whose titles are Russian can keep the menus in
 * German and still get Russian descriptions. "" = the same as the app language.
 */
object MetaLangLogic {
    /** Choices: "" (like the app) first, then language codes. */
    val CODES = listOf("", "ru", "de", "en", "uk", "tr", "pl", "es", "fr", "it", "nl", "ar", "bs", "hr", "sr")

    /** Names shown in the choice (in their own language; the first one is a translation key of the app). */
    val NAMES = listOf("Wie App-Sprache", "Русский", "Deutsch", "English", "Українська", "Türkçe", "Polski", "Español", "Français", "Italiano", "Nederlands", "العربية", "Bosanski", "Hrvatski", "Srpski")

    /** Language code used for TMDB: the chosen one, or the app's when none is chosen or the choice is unknown. */
    fun effective(chosen: String, app: String): String = chosen.trim().lowercase().takeIf { it.isNotEmpty() && it in CODES } ?: app

    /** TMDB language tag of a code ("ru" → "ru-RU"); unknown codes fall back to German like before. */
    fun tmdbTag(code: String): String = when (code) {
        "en" -> "en-US"; "tr" -> "tr-TR"; "bs" -> "bs-BA"; "hr" -> "hr-HR"; "sr" -> "sr-RS"
        "es" -> "es-ES"; "fr" -> "fr-FR"; "it" -> "it-IT"; "nl" -> "nl-NL"; "ar" -> "ar-SA"; "pl" -> "pl-PL"
        "ru" -> "ru-RU"; "uk" -> "uk-UA"; "zh" -> "zh-CN"; "pt" -> "pt-BR"
        else -> "de-DE"
    }

    /** Position of [chosen] in [CODES] (0 = like the app). */
    fun index(chosen: String): Int = CODES.indexOf(chosen.trim().lowercase()).coerceAtLeast(0)
}

package com.ipvio.stream

/**
 * Key rows of the app's own search keyboard (T-056). Without Android, testable.
 *
 * Besides the Latin rows (QWERTY), the keyboard has a Russian layer (ЙЦУКЕН, all 33 letters), so titles and channels
 * with Cyrillic names can be searched without the device keyboard.
 */
object KeyboardLogic {
    val LATIN = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    val CYRILLIC = listOf("йцукенгшщзх", "фывапролджэ", "ячсмитьбюъё")
    val SYMBOLS = listOf("-/:;()&@\"", ".,?!'+=_%", "#*<>[]{}~")
    // Special letters for other languages (German, French, Spanish, Turkish, Nordic, Slavic ...)
    val SPECIAL = listOf("äöüßéèêàâç", "ñışğčćšžđå", "øæœíóúáãõ")

    /** The three letter rows for the current layer; symbols and special letters win over the alphabet choice. */
    fun rows(symbols: Boolean, special: Boolean, cyrillic: Boolean): List<String> = when {
        symbols -> SYMBOLS
        special -> SPECIAL
        cyrillic -> CYRILLIC
        else -> LATIN
    }

    /** Label of the key that switches the alphabet: shows the alphabet you get by pressing it. */
    fun alphabetLabel(cyrillic: Boolean): String = if (cyrillic) "ABC" else "АБВ"

    /** Alphabet choice for the running app (kept while the app runs, so the search opens in the last used one). */
    @Volatile var cyrillic = false
}

package com.ipvio.stream

/**
 * Match channels to guide channels by name (T-053). Without Android, testable.
 *
 * A playlist without "tvg-id" cannot name its guide channel. Many providers still use the same channel names as
 * their guide source (measured with a Russian provider list against an XMLTV file: 2283 of 2316 names matched).
 * Both names are reduced to the same [key]; the guide channel with the most shows wins when several share a key.
 */
object EpgNameLogic {
    /** Quality words that differ between a channel row and its guide channel ("Channel HD" and "Channel"). */
    private val QUALITY = setOf("uhd", "fhd", "hd", "sd", "4k")

    /**
     * Comparable form of a channel name: lower case, "ё" as "е", only letters, digits and "+" (the time shift "+2" is
     * part of the name), quality words removed, words separated by one space. Empty = no usable name.
     */
    fun key(name: String): String {
        val sb = StringBuilder(name.length)
        for (c in name.lowercase()) {
            sb.append(
                when {
                    c == 'ё' -> 'е'
                    c.isLetterOrDigit() || c == '+' -> c
                    else -> ' '
                }
            )
        }
        return sb.split(' ').filter { it.isNotEmpty() && it !in QUALITY }.joinToString(" ")
    }

    /**
     * The guide channel to use when several channels carry the same name: the one with the most shows ([shows]).
     * Ties go to the smaller id so the choice does not change between loads. null = none has any show.
     */
    fun pick(ids: Collection<String>, shows: (String) -> Int): String? =
        ids.filter { shows(it) > 0 }.sortedWith(compareByDescending<String> { shows(it) }.thenBy { it }).firstOrNull()
}

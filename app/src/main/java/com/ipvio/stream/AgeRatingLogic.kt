package com.ipvio.stream

/**
 * Age rating (e.g. FSK) for the detail line. Pure logic without Android, so JVM tests can cover it.
 *
 * Source: TMDB ratings per country ("release_dates" for movies, "content_ratings" for series).
 * The country comes from the TMDB language of the app ("de-DE" -> DE). If that country has no rating,
 * the US rating is used and marked with "(US)". OMDb's "Rated" field is always the US rating.
 */
object AgeRatingLogic {
    private val NONE = setOf("N/A", "NOT RATED", "UNRATED", "NR")

    /** Country code from a TMDB language like "de-DE"; "US" when there is none. */
    fun country(tmdbLang: String): String = tmdbLang.substringAfter('-', "").trim().uppercase().ifEmpty { "US" }

    /**
     * Certification of a movie in one country from (release type, certification) pairs. T-080: the highest of all
     * releases (cinema, DVD, uncut, …); among certifications of unknown age the theatrical one (3) first.
     */
    fun movieCert(dates: List<Pair<Int, String>>): String? {
        val c = dates.filter { usable(it.second) }
        return highest((c.filter { it.first == 3 } + c.filter { it.first != 3 }).map { it.second })
    }

    /** T-080: [ageMin] of a title whose age rating was never asked for. */
    const val NOT_ASKED = -2
    /** T-080: [ageMin] when TMDB knows no rating of the app's country nor of the US. */
    const val UNKNOWN = -1

    /** US movie and TV ratings (and the British "U") as an age; TV-PG counts as 12, R as 18 (on the strict side). */
    private val NAMED = mapOf(
        "G" to 0, "U" to 0, "PG" to 6, "PG-13" to 12, "R" to 18, "NC-17" to 18,
        "TV-Y" to 0, "TV-G" to 0, "TV-Y7" to 6, "TV-Y7-FV" to 6, "TV-PG" to 12, "TV-14" to 14, "TV-MA" to 18
    )
    private val LEADING_NUMBER = Regex("""^(\d{1,2})(?!\d)""")

    /**
     * T-080: minimum age of a certification or a shown label: "12", "FSK 16", "16+" (Russia), "12A", "PG-13", "TV-MA (US)".
     * Null when it cannot be read.
     */
    fun minAge(cert: String?): Int? {
        var c = cert?.trim()?.uppercase() ?: return null
        c = c.replace(Regex("""\s*\([A-Z]{2}\)$"""), "").removePrefix("FSK").trim()
        if (c.isEmpty() || c in NONE) return null
        NAMED[c]?.let { return it }
        return LEADING_NUMBER.find(c)?.groupValues?.get(1)?.toIntOrNull()?.takeIf { it <= 21 }
    }

    /** T-080: the highest of several certifications of one country (e.g. one per season); unreadable ones only when no other exists. */
    fun highest(certs: List<String>): String? {
        val c = certs.filter { usable(it) }.map { it.trim() }
        if (c.isEmpty()) return null
        return c.maxByOrNull { minAge(it) ?: -1 }
    }

    /**
     * T-080: age for the kids filter: the stricter of the rating of the user's [country] and the US rating, so a series
     * with FSK 12 but TV-MA counts as 18. [UNKNOWN] when neither can be read.
     */
    fun strictest(byCountry: Map<String, String>, country: String): Int =
        maxOf(minAge(byCountry[country]) ?: UNKNOWN, minAge(byCountry["US"]) ?: UNKNOWN)

    /** Label for the user's [country], falling back to the US rating. Null when neither exists. */
    fun pick(byCountry: Map<String, String>, country: String): String? {
        byCountry[country]?.takeIf { usable(it) }?.let { return label(country, it, country) }
        if (country != "US") byCountry["US"]?.takeIf { usable(it) }?.let { return label("US", it, country) }
        return null
    }

    /** Label from OMDb's "Rated" field (US rating). */
    fun omdb(rated: String?, country: String): String? = rated?.takeIf { usable(it) }?.let { label("US", it, country) }

    /** "FSK 12" for a German number; the source country in brackets when it is not the user's country. */
    fun label(from: String, cert: String, country: String): String {
        val c = cert.trim()
        val base = if (from == "DE" && c.isNotEmpty() && c.all { it.isDigit() }) "FSK $c" else c
        return if (from == country) base else "$base ($from)"
    }

    private fun usable(s: String): Boolean = s.isNotBlank() && s.trim().uppercase() !in NONE
}

package com.ipvio.stream

/**
 * T-077: rules of the "Kids" area (cartoons and anime). Pure Kotlin, so JVM tests cover it.
 *
 * Recognition builds on [TrendLogic.isAnimation] (TMDB genre 16 or a cartoon/anime category of the provider). Anime is
 * kept apart from other cartoons: a provider category named for anime, or TMDB genre 16 with Japanese original language.
 */
object KidsLogic {
    const val NONE = 0
    const val CARTOON = 1
    const val ANIME = 2

    /** Words of a provider category for anime (lower case, part of the name). "animé" is left out: French "dessins animés" are cartoons. */
    private val ANIME_GROUPS = listOf("аниме", "anime", "アニメ", "أنمي", "动漫")

    /** True when the provider category is named for anime. */
    fun isAnimeGroup(group: String?): Boolean {
        val g = group?.lowercase() ?: return false
        return ANIME_GROUPS.any { it in g }
    }

    /** [NONE], [CARTOON] or [ANIME] for a title with these TMDB genres, original language and provider category. */
    fun classify(genreIds: List<Int>, lang: String?, group: String?): Int {
        if (isAnimeGroup(group)) return ANIME
        if (!TrendLogic.isAnimation(genreIds, group)) return NONE
        return if (16 in genreIds && lang?.trim()?.lowercase() == "ja") ANIME else CARTOON
    }

    /**
     * Top list of one row: the titles of today's TMDB trends first ([trend]), then the best rated recent ones
     * ([popular]), then the newest added ([newest]) so the row is not empty without TMDB data. Each title once, at most [limit].
     */
    fun <T> top(trend: List<T>, popular: List<T>, newest: List<T>, idOf: (T) -> String, limit: Int): List<T> {
        val seen = HashSet<String>()
        val out = ArrayList<T>(limit)
        for (l in listOf(trend, popular, newest)) for (x in l) {
            if (out.size >= limit) return out
            if (seen.add(idOf(x))) out.add(x)
        }
        return out
    }

    /** Big picture of the area: the first titles of the four top rows in turn (cartoons first), each once. */
    fun <T> heroMix(rows: List<List<T>>, idOf: (T) -> String, perRow: Int): List<T> {
        val seen = HashSet<String>()
        val out = ArrayList<T>()
        for (i in 0 until perRow) for (r in rows) r.getOrNull(i)?.let { if (seen.add(idOf(it))) out.add(it) }
        return out
    }

    /**
     * T-080: may a title with this age ([AgeRatingLogic.strictest], negative = not known) show in the kids area?
     * [limit] < 0 = no limit. Titles without a known rating show unless [hideUnrated].
     */
    fun allowed(ageMin: Int, limit: Int, hideUnrated: Boolean): Boolean = when {
        limit < 0 -> true
        ageMin < 0 -> !hideUnrated
        else -> ageMin <= limit
    }
}

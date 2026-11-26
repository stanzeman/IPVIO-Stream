package com.ipvio.stream

/**
 * "Top 10 today" (T-064): merges several ranked TMDB lists (worldwide trends of the day, popular Russian and Kazakh
 * titles) into one point list and ranks the titles of the own playlist with it. Pure Kotlin, so JVM tests cover it.
 *
 * The regional focus (T-067) applies only when the language of the title info is Russian ([focusFor]); otherwise the
 * board holds the worldwide trends alone.
 *
 * Keys are "m<tmdbId>" for movies and "t<tmdbId>" for series.
 */
object TrendLogic {
    /** Worldwide trends of the day. */
    const val W_GLOBAL = 1.0
    /** Popular Russian productions (more weight, as wanted). */
    const val W_RU = 1.4
    /** Popular Kazakh productions (same extra weight as Russia). */
    const val W_KZ = 1.4
    /** Other Russian-speaking / former Soviet countries: extra weight, but less than Russia and Kazakhstan. */
    const val W_CIS = 1.15
    /** Extra factor for a playlist title whose original language is Russian or Kazakh. */
    const val LOCAL_BOOST = 1.2
    /** Extra factor for a playlist title in the language of another former Soviet country. */
    const val CIS_BOOST = 1.1
    /** Small factor when only the production country is from the region, not the language (e.g. shot there in English). */
    const val COUNTRY_BOOST = 1.05
    /**
     * Focus name stored with the board; "" = worldwide only. "ru2" = lists by original language: a board saved with the
     * older country lists ("ru") is not fresh any more and is loaded again at once.
     */
    const val FOCUS_RU = "ru2"

    /** Former Soviet countries except RU and KZ (ISO 3166-1), for the small country bonus ([COUNTRY_BOOST]). */
    val CIS_COUNTRIES = listOf("UA", "BY", "UZ", "KG", "TJ", "TM", "AZ", "AM", "GE", "MD", "LT", "LV", "EE")
    private val CIS_SET = CIS_COUNTRIES.toSet()
    private val CIS_LANGS = setOf("uk", "be", "uz", "ky", "tg", "tk", "az", "hy", "ka", "lt", "lv", "et")
    /** Languages asked at TMDB one by one with [W_CIS] (the ones with a noticeable film and series output). */
    val CIS_QUERY_LANGS = listOf("uk", "be", "uz", "ka", "hy", "az", "ky")

    /** TMDB `discover` lists per original language: (language, weight, pages). Empty without the RU focus. */
    fun regionLists(focus: String): List<Triple<String, Double, Int>> =
        if (focus != FOCUS_RU) emptyList()
        else listOf(Triple("ru", W_RU, 2), Triple("kk", W_KZ, 1)) + CIS_QUERY_LANGS.map { Triple(it, W_CIS, 1) }

    /** Focus for the chosen language of the title info ([Lang.metaCode] in the app): Russian = RU/KZ focus, else none. */
    fun focusFor(metaCode: String): String = if (metaCode.trim().lowercase() == "ru") FOCUS_RU else ""
    /** Rank damping: place 1 gets w/(K+1), place 50 w/(K+50); a title in several lists adds up. */
    const val K = 20.0
    /** A saved board is used for up to 7 days when TMDB cannot be reached. */
    const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

    class Source(val weight: Double, val keys: List<String>)

    /**
     * Result of one day. [points] per key, [titles] maps title keys ([titleKey]: kind + normalized title) to a key, for
     * playlist titles whose TMDB id is not known yet. [lang] is the TMDB language the lists were loaded in (T-073), so the
     * titles carry the names the playlist uses (e.g. Russian) besides the original names.
     */
    class Board(
        val day: String, val savedAt: Long, val points: Map<String, Double>, val titles: Map<String, String>, val focus: String = "",
        val lang: String = ""
    ) {
        fun isEmpty() = points.isEmpty()
        /** Same content as a new object: the home screen recomputes its lists (it compares the board by identity). */
        fun renewed() = Board(day, savedAt, points, titles, focus, lang)
    }

    val EMPTY = Board("", 0L, emptyMap(), emptyMap())

    fun norm(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }

    fun key(isTv: Boolean, tmdbId: Int): String = (if (isTv) "t" else "m") + tmdbId

    /**
     * Key of [Board.titles] (T-073): the kind is part of it, so a series never takes the points of a movie with the same
     * name (the series "Мастер и Маргарита" of 2005 got the points of the movie of 2024).
     */
    fun titleKey(isTv: Boolean, title: String): String = (if (isTv) "t:" else "m:") + norm(title)

    /** Reciprocal-rank fusion: every list adds weight/(K + place) for each of its titles. */
    fun fuse(sources: List<Source>): Map<String, Double> {
        val out = HashMap<String, Double>()
        for (s in sources) {
            val seen = HashSet<String>()
            s.keys.forEachIndexed { idx, k ->
                if (k.isNotEmpty() && seen.add(k)) out[k] = (out[k] ?: 0.0) + s.weight / (K + idx + 1)
            }
        }
        return out
    }

    /** True for Russian or Kazakh original language or production country. */
    fun isLocal(lang: String?, countries: List<String>): Boolean =
        lang?.lowercase() in LOCAL_LANGS || countries.any { it.uppercase() in LOCAL_COUNTRIES }

    private val LOCAL_LANGS = setOf("ru", "kk")
    private val LOCAL_COUNTRIES = setOf("RU", "KZ")

    /** True for another former Soviet country or its language (not RU/KZ, see [isLocal]). */
    fun isCis(lang: String?, countries: List<String>): Boolean =
        lang?.lowercase() in CIS_LANGS || countries.any { it.uppercase() in CIS_SET }

    /**
     * Ranking factor of a playlist title by its original language: 1.2 Russian/Kazakh, 1.1 another former Soviet language,
     * 1.05 when only a production country is from there, 1.0 otherwise or without focus.
     */
    fun boost(focus: String, lang: String?, countries: List<String>): Double {
        if (focus != FOCUS_RU) return 1.0
        val l = lang?.lowercase()
        return when {
            l in LOCAL_LANGS -> LOCAL_BOOST
            l in CIS_LANGS -> CIS_BOOST
            isLocal(null, countries) || isCis(null, countries) -> COUNTRY_BOOST
            else -> 1.0
        }
    }

    /** The saved board is from today, for the same focus and the same TMDB language: no new request. */
    fun fresh(b: Board, today: String, focus: String = "", lang: String = ""): Boolean =
        b.day == today && b.focus == focus && b.lang == lang && !b.isEmpty()

    /** The saved board may still be shown (TMDB not reachable): not older than [MAX_AGE_MS], clock not gone back. */
    fun usable(b: Board, now: Long): Boolean = !b.isEmpty() && now - b.savedAt in 0..MAX_AGE_MS

    /**
     * Ranks playlist titles of one kind ([tv] = series): [keyOf] gives the TMDB key (null = unknown), [titleOf] the title for
     * the fallback, [boostOf] the extra factor (see [boost]). Every key counts once (the first playlist entry wins), best
     * first, at most [limit]. With several seasons of one series as separate playlist titles, the one with the highest
     * [seasonOf] is shown.
     *
     * T-073: the TMDB number decides. The name counts only while the number is not known yet (see [candidates]), and only
     * within the same kind. A title whose number is known but not on the board is not in the trend, whatever its name.
     */
    fun <T> rank(
        items: List<T>, board: Board, tv: Boolean, keyOf: (T) -> String?, titleOf: (T) -> String, boostOf: (T) -> Double, limit: Int,
        seasonOf: (T) -> Int = { 0 }
    ): List<T> {
        if (board.isEmpty() || items.isEmpty()) return emptyList()
        val best = HashMap<String, Pair<T, Double>>()
        for (it in items) {
            val known = keyOf(it)
            val k = if (known != null) known.takeIf { k -> board.points.containsKey(k) } ?: continue
                else board.titles[titleKey(tv, titleOf(it))] ?: continue
            val p = (board.points[k] ?: continue) * boostOf(it)
            // T-074: several seasons listed as separate titles: the newest season shows (else the first entry)
            val prev = best[k]
            if (prev == null || seasonOf(it) > seasonOf(prev.first)) best[k] = it to p
        }
        return best.values.sortedByDescending { it.second }.take(limit).map { it.first }
    }

    /**
     * Playlist titles to check at TMDB first (T-073): their name matches a trend title of the same kind, but the TMDB
     * number is not known yet or differs (possibly a wrong match, like "Roots" for the Russian "Корни"). Best trend points
     * first, at most [limit]. A title whose known number is on the board needs no check.
     */
    fun <T> candidates(
        items: List<T>, board: Board, tv: Boolean, keyOf: (T) -> String?, titleOf: (T) -> String, limit: Int
    ): List<T> {
        if (board.isEmpty() || items.isEmpty()) return emptyList()
        val out = ArrayList<Pair<T, Double>>()
        for (it in items) {
            val k = board.titles[titleKey(tv, titleOf(it))] ?: continue
            val known = keyOf(it)
            if (known != null && board.points.containsKey(known)) continue
            out.add(it to (board.points[k] ?: 0.0))
        }
        return out.sortedByDescending { it.second }.take(limit).map { it.first }
    }
}

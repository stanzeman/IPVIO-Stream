package com.ipvio.stream

/**
 * "Top 10 today" (T-064): merges several ranked TMDB lists (worldwide trends of the day, popular Russian and Kazakh
 * titles) into one point list and ranks the titles of the own playlist with it. Pure Kotlin, so JVM tests cover it.
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
    /** Extra factor for a playlist title whose original language is Russian or Kazakh, or that was made in RU or KZ. */
    const val LOCAL_BOOST = 1.2
    /** Rank damping: place 1 gets w/(K+1), place 50 w/(K+50); a title in several lists adds up. */
    const val K = 20.0
    /** A saved board is used for up to 7 days when TMDB cannot be reached. */
    const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

    class Source(val weight: Double, val keys: List<String>)

    /**
     * Result of one day. [points] per key, [titles] maps normalized titles ([norm]) to a key, for playlist titles whose
     * TMDB id is not known yet.
     */
    class Board(val day: String, val savedAt: Long, val points: Map<String, Double>, val titles: Map<String, String>) {
        fun isEmpty() = points.isEmpty()
    }

    val EMPTY = Board("", 0L, emptyMap(), emptyMap())

    fun norm(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }

    fun key(isTv: Boolean, tmdbId: Int): String = (if (isTv) "t" else "m") + tmdbId

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

    /** The saved board is from today: no new request. */
    fun fresh(b: Board, today: String): Boolean = b.day == today && !b.isEmpty()

    /** The saved board may still be shown (TMDB not reachable): not older than [MAX_AGE_MS], clock not gone back. */
    fun usable(b: Board, now: Long): Boolean = !b.isEmpty() && now - b.savedAt in 0..MAX_AGE_MS

    /**
     * Ranks playlist titles: [keyOf] gives the TMDB key (null = unknown), [titleOf] the title for the fallback, [local]
     * whether the boost applies. Every key counts once (the first playlist entry wins), best first, at most [limit].
     */
    fun <T> rank(
        items: List<T>, board: Board, keyOf: (T) -> String?, titleOf: (T) -> String, local: (T) -> Boolean, limit: Int
    ): List<T> {
        if (board.isEmpty() || items.isEmpty()) return emptyList()
        val best = HashMap<String, Pair<T, Double>>()
        for (it in items) {
            val k = keyOf(it)?.takeIf { k -> board.points.containsKey(k) } ?: board.titles[norm(titleOf(it))] ?: continue
            val p = (board.points[k] ?: continue) * (if (local(it)) LOCAL_BOOST else 1.0)
            if (!best.containsKey(k)) best[k] = it to p
        }
        return best.values.sortedByDescending { it.second }.take(limit).map { it.first }
    }
}

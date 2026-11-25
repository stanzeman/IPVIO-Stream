package com.ipvio.stream

import kotlin.math.log10
import kotlin.math.min

/**
 * Lists of the movies and series tabs (T-065, T-066): "Popular & new", "New titles" and "Russian & Kazakh".
 * Pure Kotlin, so JVM tests cover it.
 */
object HomeListsLogic {
    /** Votes at which a rating counts half (weighted rating, see [weighted]). */
    const val M = 50
    /** "New" = newest year not older than this many years before the current year. */
    const val RECENT_YEARS = 2
    /** With fewer new titles the list is filled up with older ones. */
    const val MIN_SIZE = 12

    /** What the lists need of a title (from the metadata). [newest] is the key of [SeriesAirLogic.newestKey]. */
    class Info(val score: Double?, val votes: Int, val newest: String, val local: Boolean)

    /** Weighted rating: few votes pull the score towards the mean [c]. */
    fun weighted(r: Double, votes: Int, c: Double, m: Int = M): Double {
        val v = votes.coerceAtLeast(0).toDouble()
        return (v / (v + m)) * r + (m / (v + m)) * c
    }

    /** Popularity factor 0.6 to 1.0 from the number of votes (10,000 votes and more = 1.0). */
    fun popularity(votes: Int): Double = 0.6 + 0.4 * min(1.0, log10(votes.coerceAtLeast(0) + 1.0) / 4.0)

    fun yearOf(newest: String): Int? = SeriesAirLogic.yearOf(newest)

    /**
     * "Popular & new": titles of the last [RECENT_YEARS] years by weighted rating times popularity; filled up with older
     * titles (same order) when fewer than [MIN_SIZE] are new. Titles without a score are left out.
     */
    fun <T> popularNew(items: List<T>, info: (T) -> Info?, nowYear: Int): List<T> {
        val rated = items.mapNotNull { i -> info(i)?.takeIf { it.score != null }?.let { i to it } }
        if (rated.isEmpty()) return emptyList()
        val c = rated.sumOf { it.second.score!! } / rated.size
        fun value(x: Info) = weighted(x.score!!, x.votes, c) * popularity(x.votes)
        val (recent, older) = rated.partition { (yearOf(it.second.newest) ?: 0) >= nowYear - RECENT_YEARS }
        val r = recent.sortedByDescending { value(it.second) }.map { it.first }
        if (r.size >= MIN_SIZE) return r
        return r + older.sortedByDescending { value(it.second) }.map { it.first }.take(MIN_SIZE * 4)
    }

    /**
     * "New titles": newest first by release year (series: latest episode). [items] is in playlist order (newest added
     * last), so with the same key the title added last comes first. Titles without a year are left out.
     */
    fun <T> newestFirst(items: List<T>, info: (T) -> Info?): List<T> =
        items.asReversed().mapNotNull { i -> info(i)?.newest?.takeIf { it.isNotEmpty() }?.let { i to it } }
            .sortedByDescending { it.second }.map { it.first }

    /** "Russian & Kazakh": only titles made in RU/KZ or in Russian/Kazakh, newest first; titles without a year at the end. */
    fun <T> localNewest(items: List<T>, info: (T) -> Info?): List<T> {
        val local = items.filter { info(it)?.local == true }
        val dated = newestFirst(local, info)
        if (dated.size == local.size) return dated
        // By identity: playlist entries are data classes with episode lists, comparing them by value is slow
        val seen: MutableSet<T> = java.util.Collections.newSetFromMap(java.util.IdentityHashMap())
        seen.addAll(dated)
        return dated + local.asReversed().filter { it !in seen }
    }
}

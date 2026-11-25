package com.ipvio.stream

/**
 * Date of the latest episode of a series (T-063). Without Android, testable.
 *
 * TMDB names it in the details of a series (`last_air_date`), not in the search results the genre detection uses. The app
 * asks for it once per series in a background phase and again later: running series every 7 days, older ones every
 * 90 days. Movies and series without it fall back to their year.
 */
object SeriesAirLogic {
    private const val DAY = 24 * 3_600_000L
    const val REFRESH_RUNNING_MS = 7 * DAY
    const val REFRESH_OLD_MS = 90 * DAY

    /** Year of a date "2026-11-20" (or of a year "2026"); null = unusable. */
    fun yearOf(date: String?): Int? = date?.trim()?.take(4)?.toIntOrNull()?.takeIf { it in 1850..2200 }

    /** true = ask TMDB (again) for the latest episode ([lastAir] stored date, [checkedAt] when it was asked, 0 = never). */
    fun due(lastAir: String?, checkedAt: Long, now: Long, nowYear: Int): Boolean {
        if (checkedAt <= 0L || now < checkedAt) return true
        val age = now - checkedAt
        val y = yearOf(lastAir)
        val running = y == null || y >= nowYear - 1
        return age >= if (running) REFRESH_RUNNING_MS else REFRESH_OLD_MS
    }

    /**
     * Sort key "newest first" of a title: series by their latest episode, otherwise by the year (as "YYYY-00-00", so a
     * date in the same year sorts above a bare year). Empty = unknown (sorts last).
     */
    fun newestKey(isSeries: Boolean, lastAir: String?, year: String?): String {
        if (isSeries) lastAir?.trim()?.takeIf { yearOf(it) != null && it.length >= 10 }?.let { return it.take(10) }
        val y = yearOf(year) ?: return ""
        return "$y-00-00"
    }
}

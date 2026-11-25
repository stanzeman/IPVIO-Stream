package com.ipvio.stream

import android.content.Context

/** Last sort picked in a category view, per profile and per type (movies, series, live TV), see [BrowseSortLogic] (T-062). */
object BrowseSortPrefs {
    private fun prefs(c: Context) = Profiles.sp(c, "ipvio_browsesort")

    /** Remembered sort for this type, -1 = none. */
    fun get(c: Context, kind: Kind?): Int = prefs(c).getInt(kind?.name ?: "ALL", -1)

    fun set(c: Context, kind: Kind?, sort: Int) { prefs(c).edit().putInt(kind?.name ?: "ALL", sort).apply() }
}

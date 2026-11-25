package com.ipvio.stream

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Most watched live channels (T-070): watch time per channel, per profile (rules in [LiveWatchLogic]).
 * The player calls [tick] every few seconds while a live channel plays; storage is written only when a viewing ends
 * (channel change, player closed, app in the background) and only if it lasted at least 30 seconds.
 * Private categories are never recorded (like the history).
 */
object LiveWatch {
    private var map: Map<String, LiveWatchLogic.Entry> = emptyMap()
    private var inited = false
    private val session = LiveWatchLogic.Session()
    /** Changes whenever the stored list changes, so the rows are recomputed. */
    var version by mutableIntStateOf(0)
        private set

    private fun prefs(c: Context) = Profiles.sp(c, "ipvio_livewatch")

    fun init(c: Context) {
        if (inited) return
        inited = true
        map = LiveWatchLogic.parse(prefs(c).getString("v", null))
    }

    /** Deletes all watch times (together with "clear history"). */
    fun clear(c: Context) {
        session.finish()
        map = emptyMap()
        inited = true
        prefs(c).edit().clear().apply()
        version++
    }

    /** [sec] seconds of channel [id] were played. Main thread. */
    fun tick(c: Context, id: String, sec: Long) {
        if (PrivateCats.isPrivateId(id)) return
        session.tick(id, sec)?.let { store(c, it.first, it.second) }
    }

    /** Ends the running viewing (channel left, player closed or paused). Main thread. */
    fun finish(c: Context) {
        session.finish()?.let { store(c, it.first, it.second) }
    }

    private fun store(c: Context, id: String, sec: Long) {
        init(c)
        val next = LiveWatchLogic.add(map, id, sec, System.currentTimeMillis()) ?: return
        map = next
        prefs(c).edit().putString("v", LiveWatchLogic.serialize(next)).apply()
        version++
    }

    /** Top channels among [byId] (adult filter by [keep]). */
    fun <T> top(c: Context, byId: (String) -> T?, keep: (T) -> Boolean = { true }, n: Int = LiveWatchLogic.TOP): List<T> {
        init(c)
        return LiveWatchLogic.top(map, System.currentTimeMillis(), byId, keep, n)
    }
}

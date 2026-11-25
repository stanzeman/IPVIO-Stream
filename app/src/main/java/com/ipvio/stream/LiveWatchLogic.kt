package com.ipvio.stream

/**
 * Most watched live channels (T-068). Without Android, testable.
 *
 * Every live channel collects its watch time in seconds. A viewing counts only from [MIN_SEC] seconds on, so zapping
 * through channels does not change the list. Older watch time loses weight: it halves every [HALF_LIFE_DAYS] days, so a
 * channel that is no longer watched slowly drops out. At most [MAX_ENTRIES] channels are kept.
 */
object LiveWatchLogic {
    /** A viewing shorter than this (seconds) is not counted. */
    const val MIN_SEC = 30L
    /** Watch time halves after this many days without new viewing. */
    const val HALF_LIFE_DAYS = 30.0
    /** Channels kept in storage (the ones with the lowest score are dropped). */
    const val MAX_ENTRIES = 200
    /** Size of the top list. */
    const val TOP = 10

    private const val DAY_MS = 86_400_000.0

    /** Stored state of one channel: [score] = weighted watch seconds at the time [ts] (epoch ms). */
    data class Entry(val score: Double, val ts: Long)

    /** Score of [e] at [now] (decayed). */
    fun decayed(e: Entry, now: Long): Double {
        val days = (now - e.ts).coerceAtLeast(0L) / DAY_MS
        return e.score * Math.pow(0.5, days / HALF_LIFE_DAYS)
    }

    /**
     * Adds one viewing of [sec] seconds of channel [id] at [now]. Viewings below [MIN_SEC] and empty ids change nothing.
     * Returns the new map (the input is not changed) or null if nothing changed.
     */
    fun add(map: Map<String, Entry>, id: String, sec: Long, now: Long): Map<String, Entry>? {
        if (sec < MIN_SEC || !validId(id)) return null
        val old = map[id]
        val out = HashMap(map)
        out[id] = Entry((if (old == null) 0.0 else decayed(old, now)) + sec, now)
        return prune(out, now)
    }

    /** Keeps the [max] channels with the highest decayed score. */
    fun prune(map: Map<String, Entry>, now: Long, max: Int = MAX_ENTRIES): Map<String, Entry> {
        if (map.size <= max) return map
        val keep = map.entries.sortedByDescending { decayed(it.value, now) }.take(max)
        return keep.associate { it.key to it.value }
    }

    /** Channel ids by decayed score, highest first (ties: most recent first). */
    fun ranked(map: Map<String, Entry>, now: Long): List<String> =
        map.entries.sortedWith(compareByDescending<Map.Entry<String, Entry>> { decayed(it.value, now) }.thenByDescending { it.value.ts })
            .map { it.key }

    /**
     * Top [n] channels that exist in [byId] and pass [keep] (parental and private filter). Ids that are no longer in the
     * playlist are skipped, so the list fills up with the next ones.
     */
    fun <T> top(map: Map<String, Entry>, now: Long, byId: (String) -> T?, keep: (T) -> Boolean = { true }, n: Int = TOP): List<T> {
        val out = ArrayList<T>(n)
        for (id in ranked(map, now)) {
            if (out.size >= n) break
            val it = byId(id) ?: continue
            if (keep(it)) out.add(it)
        }
        return out
    }

    /** Ids with tab or line break are not stored (they would break the line format). */
    fun validId(id: String): Boolean = id.isNotBlank() && id.none { it == '\t' || it == '\n' || it == '\r' }

    /** One line per channel: id TAB score TAB ts. */
    fun serialize(map: Map<String, Entry>): String =
        map.entries.joinToString("\n") { it.key + "\t" + Math.round(it.value.score * 10) / 10.0 + "\t" + it.value.ts }

    /** Reads [serialize] output; broken lines are skipped. */
    fun parse(s: String?): Map<String, Entry> {
        if (s.isNullOrBlank()) return emptyMap()
        val out = HashMap<String, Entry>()
        for (line in s.split("\n")) {
            val p = line.split("\t")
            if (p.size != 3 || !validId(p[0])) continue
            val score = p[1].toDoubleOrNull() ?: continue
            val ts = p[2].toLongOrNull() ?: continue
            if (score.isNaN() || score <= 0.0) continue
            out[p[0]] = Entry(score, ts)
        }
        return out
    }

    /**
     * Running viewing in the player. [tick] is called every few seconds while a live channel plays; it returns the
     * finished viewing (previous id and its seconds) when the channel changed, otherwise null. [finish] ends the viewing.
     */
    class Session {
        var id: String? = null
            private set
        var sec = 0L
            private set

        fun tick(cur: String, add: Long): Pair<String, Long>? {
            val done = if (id != null && id != cur) finish() else null
            id = cur
            sec += add
            return done
        }

        fun finish(): Pair<String, Long>? {
            val i = id
            val s = sec
            id = null; sec = 0L
            return if (i != null && s > 0) i to s else null
        }
    }
}

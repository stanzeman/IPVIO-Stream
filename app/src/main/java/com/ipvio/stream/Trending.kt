package com.ipvio.stream

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * "Top 10 today" (T-064): loads the trends of the day once per day from TMDB and keeps them in a file.
 *
 * Lists: worldwide trends of the day (movies, series, 3 pages each). Only with Russian title info (T-067): popular
 * Russian and Kazakh movies of the last years and series that aired recently, plus the same for the other former Soviet
 * countries with less weight. About 12 requests per day with a pause between them; on 429 or an
 * error the loading stops and the saved board is kept (up to 7 days, see [TrendLogic.usable]).
 */
object Trending {
    private val client = Net.base
    private const val FILE = "trend2.json"
    private const val PAUSE_MS = 250L
    /** No new attempt within 30 minutes after a failed or incomplete load. */
    private const val RETRY_MS = 30L * 60 * 1000
    @Volatile private var cache: TrendLogic.Board? = null
    @Volatile private var lastTry = 0L
    @Volatile private var lastFocus: String? = null

    private fun day(offsetDays: Int = 0): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, offsetDays)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }

    fun norm(s: String): String = TrendLogic.norm(s)

    private class Stop : Exception()

    suspend fun board(ctx: Context): TrendLogic.Board {
        val today = day()
        val now = System.currentTimeMillis()
        val focus = TrendLogic.focusFor(Lang.metaCode())
        val saved = cache ?: withContext(Dispatchers.IO) { read(ctx) }?.also { cache = it }
        if (saved != null && TrendLogic.fresh(saved, today, focus)) return saved
        val old = saved?.takeIf { TrendLogic.usable(it, now) } ?: TrendLogic.EMPTY
        // A changed language of the title info loads at once (no 30-minute wait)
        if (!MetaRepo.enabled() || (focus == lastFocus && now - lastTry in 0 until RETRY_MS)) return old
        lastTry = now
        lastFocus = focus
        val loaded = withContext(Dispatchers.IO) { load(today, now, focus) }
        if (loaded != null && !loaded.isEmpty()) {
            cache = loaded
            withContext(Dispatchers.IO) { runCatching { write(ctx, loaded) } }
            return loaded
        }
        return old
    }

    private fun load(today: String, now: Long, focus: String): TrendLogic.Board? {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val since = day(-60)
        var failed = false
        val key = Config.TMDB_KEY
        val base = "https://api.themoviedb.org/3"
        val titles = HashMap<String, String>()
        val sources = ArrayList<TrendLogic.Source>()
        fun list(url: String, isTv: Boolean, pages: Int): List<String> {
            val out = ArrayList<String>()
            for (p in 1..pages) {
                val a = fetch("$url&api_key=$key&page=$p") ?: run { failed = true; null } ?: break
                for (x in 0 until a.length()) {
                    val o = a.optJSONObject(x) ?: continue
                    val id = o.optInt("id")
                    if (id <= 0) continue
                    val k = TrendLogic.key(isTv, id)
                    out.add(k)
                    for (f in listOf("title", "name", "original_title", "original_name")) {
                        val t = o.optString(f)
                        if (t.isNotBlank()) titles.putIfAbsent(TrendLogic.norm(t), k)
                    }
                }
                if (a.length() < 20) break
            }
            return out
        }
        try {
            sources += TrendLogic.Source(TrendLogic.W_GLOBAL, list("$base/trending/movie/day?language=en-US", false, 3))
            sources += TrendLogic.Source(TrendLogic.W_GLOBAL, list("$base/trending/tv/day?language=en-US", true, 3))
            val regions = if (focus != TrendLogic.FOCUS_RU) emptyList() else listOf(
                Triple("RU", TrendLogic.W_RU, 2), Triple("KZ", TrendLogic.W_KZ, 1),
                Triple(TrendLogic.CIS_COUNTRIES.joinToString("|"), TrendLogic.W_CIS, 1)
            )
            for ((c, w, pages) in regions) {
                sources += TrendLogic.Source(w, list("$base/discover/movie?with_origin_country=$c&sort_by=popularity.desc&primary_release_date.gte=${year - 2}-01-01", false, pages))
                sources += TrendLogic.Source(w, list("$base/discover/tv?with_origin_country=$c&sort_by=popularity.desc&air_date.gte=$since", true, pages))
            }
        } catch (e: Stop) {
            // Too many requests: keep what was saved, try again on the next start of the home screen
            return null
        }
        val points = TrendLogic.fuse(sources)
        if (points.isEmpty()) return null
        // Incomplete (a page failed): show it, but not as "today", so a later start loads again
        return TrendLogic.Board(if (failed) "" else today, now, points, titles.filterValues { it in points }, focus)
    }

    /** One page; null = error. A 429 stops the whole run. */
    private fun fetch(url: String): JSONArray? {
        Thread.sleep(PAUSE_MS)
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (r.code == 429) throw Stop()
                if (!r.isSuccessful) null else r.body?.string()?.let { JSONObject(it).optJSONArray("results") }
            }
        } catch (e: Stop) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun read(ctx: Context): TrendLogic.Board? = try {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) null else {
            val o = JSONObject(f.readText())
            val p = o.getJSONObject("p")
            val points = HashMap<String, Double>()
            for (k in p.keys()) points[k] = p.getDouble(k)
            val t = o.optJSONObject("t") ?: JSONObject()
            val titles = HashMap<String, String>()
            for (k in t.keys()) titles[k] = t.getString(k)
            TrendLogic.Board(o.getString("day"), o.getLong("at"), points, titles, o.optString("f"))
        }
    } catch (e: Exception) {
        null
    }

    private fun write(ctx: Context, b: TrendLogic.Board) {
        val o = JSONObject().put("day", b.day).put("at", b.savedAt).put("f", b.focus)
            .put("p", JSONObject(b.points)).put("t", JSONObject(b.titles))
        val tmp = File(ctx.filesDir, "$FILE.tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(File(ctx.filesDir, FILE))
    }
}

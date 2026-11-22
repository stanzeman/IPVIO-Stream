package com.ipvio.stream

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.Writer
import java.util.concurrent.TimeUnit

/**
 * Connection to a key-based video portal (see [PortalLogic], T-036): categories, movies and series as a list, episodes
 * when a series is opened. Every request is a JSON POST with the key. The key is never written to a log or an error
 * text; [note] holds only a short reason such as "HTTP 429".
 *
 * Loading is gentle on the portal: a pause between pages, a wait on "429 Too Many Requests", a cool-down after several
 * categories in a row without an answer. Only the newest [MAX_PAGES] pages (30 titles each) of every category are
 * loaded: big categories have thousands of titles (see the task file for the numbers).
 */
object PortalApi {
    private val client: OkHttpClient = Net.base.newBuilder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()
    private val JSON = "application/json; charset=UTF-8".toMediaType()

    /** Pages (30 titles each) loaded per category when the playlist is built. */
    private const val MAX_PAGES = 20
    /** Pause between two pages so the portal does not block. */
    private const val PAGE_PAUSE_MS = 150L
    private const val GIVE_UP_AFTER = 3
    private const val MAX_COOLDOWNS = 2
    private const val COOLDOWN_MS = 30_000L

    /** Last problem in short form (code or kind only, never address or key). */
    @Volatile var note = ""
        private set

    /** One request; null = no usable answer (the reason is in [note]). On 429 it waits as the portal asks, once. */
    private fun post(src: PortalLogic.Source, fields: Map<String, Any>): JSONObject? {
        val body = JSONObject().also { o ->
            for ((k, v) in fields) o.put(k, v)
            o.put("key", src.key)
        }.toString()
        val req = Request.Builder().url(src.base).header("Accept", "application/json").post(body.toRequestBody(JSON)).build()
        for (round in 0 until 2) {
            val res = try {
                client.newCall(req).execute().use { r ->
                    val wait = if (r.code == 429) (r.header("Retry-After")?.trim()?.toLongOrNull() ?: 2L).coerceIn(1L, 6L) else 0L
                    Triple(r.code, r.body?.string().orEmpty(), wait)
                }
            } catch (e: Exception) {
                note = "no connection"
                return null
            }
            if (res.first == 429 && round == 0) { Thread.sleep(res.third * 1000); continue }
            if (res.first !in 200..299) { note = "HTTP ${res.first}"; return null }
            val o = runCatching { JSONObject(res.second) }.getOrNull()
            if (o == null) { note = "invalid answer"; return null }
            note = ""
            return o
        }
        note = "HTTP 429"
        return null
    }

    private fun entry(o: JSONObject): PortalLogic.Entry = PortalLogic.Entry(
        o.optString("type"), o.optString("title"), o.optString("img"), o.optInt("fid", 0),
        o.optString("url"), o.optInt("year", 0), o.optString("description")
    )

    private fun page(o: JSONObject): PortalLogic.Page {
        val a = o.optJSONArray("items")
        val entries = ArrayList<PortalLogic.Entry>()
        var next: Int? = null
        if (a != null) for (i in 0 until a.length()) {
            val it = a.optJSONObject(i) ?: continue
            when (it.optString("type")) {
                "stream", "multistream" -> entries.add(entry(it))
                "next" -> next = it.optJSONObject("request")?.optInt("offset", -1)?.takeIf { n -> n > 0 }
            }
        }
        return PortalLogic.Page(entries, next, o.optInt("count", -1))
    }

    private fun requestMap(o: JSONObject?): Map<String, Any> {
        val m = LinkedHashMap<String, Any>()
        if (o != null) for (k in o.keys()) m[k] = o.get(k)
        return m
    }

    private fun categories(root: JSONObject): List<PortalLogic.Cat> {
        val a = root.optJSONArray("items") ?: return emptyList()
        val out = ArrayList<PortalLogic.Cat>()
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            if (o.optString("type") != "category") continue
            PortalLogic.cat(o.optString("title"), requestMap(o.optJSONObject("request")))?.let { out.add(it) }
        }
        return out
    }

    /** Titles of a category: the first [MAX_PAGES] pages, newest first. [ok] false = not even the first page came. */
    private class Loaded(val rows: List<PortalLogic.Entry>, val ok: Boolean)

    private fun loadCategory(src: PortalLogic.Source, c: PortalLogic.Cat, check: () -> Unit): Loaded {
        val rows = ArrayList<PortalLogic.Entry>()
        var req: Map<String, Any> = c.request
        var offset = 0
        repeat(MAX_PAGES) {
            check()
            val o = post(src, req) ?: return Loaded(rows, rows.isNotEmpty())
            val p = page(o)
            rows.addAll(p.entries)
            val nx = PortalLogic.nextOffset(p.next, offset) ?: return Loaded(rows, true)
            offset = nx
            req = PortalLogic.nextRequest(c.request, nx)
            Thread.sleep(PAGE_PAUSE_MS)
        }
        return Loaded(rows, true)
    }

    /**
     * Write the list to [out] (M3U): the newest titles of every category (movies and series placeholders). [onPhase] =
     * progress, [check] throws on abort. Return value: number of entries.
     */
    fun write(src: PortalLogic.Source, out: Writer, onPhase: (String) -> Unit, check: () -> Unit): Int {
        onPhase("Zugang wird geprüft …")
        val root = post(src, emptyMap()) ?: throw PlaylistError(
            "Das Portal antwortet nicht" + (if (note.isNotEmpty()) " ($note)" else "") + ". Adresse und Key prüfen."
        )
        val cats = categories(root)
        if (cats.isEmpty()) throw PlaylistError("Das Portal hat keine Kategorien geliefert. Adresse und Key prüfen.")
        out.write("#EXTM3U\n")
        onPhase("Kategorien werden geladen …")
        var n = 0
        val seen = HashSet<Int>()
        var failedInRow = 0
        var cooldowns = 0
        fun wait() { var w = 0L; while (w < COOLDOWN_MS) { check(); Thread.sleep(1000); w += 1000 } }
        for ((idx, c) in cats.withIndex()) {
            check()
            if (failedInRow >= GIVE_UP_AFTER && cooldowns < MAX_COOLDOWNS) {
                cooldowns++; failedInRow = 0
                onPhase("Portal antwortet nicht, neuer Versuch in ${COOLDOWN_MS / 1000} s …")
                wait()
            }
            if (failedInRow >= GIVE_UP_AFTER) continue
            val l = loadCategory(src, c, check)
            failedInRow = if (l.ok) 0 else failedInRow + 1
            // The portal sends the newest first; the app reads "new" from the end of the list: write oldest first
            for (i in l.rows.indices.reversed()) {
                val e = l.rows[i]
                if (e.fid <= 0 || !seen.add(e.fid)) continue
                val line = if (e.type == "multistream") PortalLogic.seriesEntry(src, e, c.title) else PortalLogic.movieEntry(e, c.title)
                if (line != null) { out.write(line); out.write("\n"); n++ }
            }
            onPhase("Kategorien werden geladen (${idx + 1}/${cats.size}) …")
        }
        out.flush()
        if (n == 0) {
            val why = if (note.isNotEmpty()) " ($note)" else ""
            throw PlaylistError("Das Portal hat keine Titel geliefert$why. Adresse und Key prüfen.")
        }
        return n
    }

    private fun sourceOf(id: Int): PortalLogic.Source? =
        PlaylistStore.lists.asSequence().mapNotNull { PortalLogic.parse(it.url) }.firstOrNull { it.id == id }

    /**
     * Episodes of a series ([url] = placeholder "portal://…/series/…"). null = the portal does not answer;
     * an empty list = series without playable episodes.
     */
    fun episodeList(url: String): List<Episode>? {
        val ref = PortalLogic.seriesRef(url) ?: return null
        val src = sourceOf(ref.sourceId) ?: return null
        val o = post(src, mapOf("cmd" to "flick", "fid" to ref.fid)) ?: return null
        val a = o.optJSONArray("items") ?: return emptyList()
        val entries = ArrayList<PortalLogic.Entry>()
        for (i in 0 until a.length()) a.optJSONObject(i)?.let { entries.add(entry(it)) }
        return PortalLogic.episodes(entries)
    }
}

package com.ipvio.stream

import android.content.Context
import android.util.Base64
import android.util.Xml
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

private val FMT = object : ThreadLocal<HashMap<String, SimpleDateFormat>>() {
    override fun initialValue() = HashMap<String, SimpleDateFormat>()
}

private fun fmt(pattern: String): SimpleDateFormat {
    val loc = Lang.locale()
    val m = FMT.get()!!
    val f = m.getOrPut(pattern + "|" + loc.toLanguageTag()) { SimpleDateFormat(pattern, loc) }
    f.timeZone = epgZone()
    return f
}

/** Time zone for all clock times (setting "Time zone" under Language & Region; default: system time of the device). */
fun epgZone(): TimeZone = General.zone()

/** Clock time in the chosen format (24 h / 12 h) and in the chosen time zone. The formatters are reused per thread. */
fun hhmm(sec: Long): String = fmt(SettingsLogic.timePattern(General.timeFmt, General.sys24)).format(Date(sec * 1000))

/** Day "Mo 19.10." in the chosen time zone and language. */
fun dayLabel(sec: Long): String = fmt("EE dd.MM.").format(Date(sec * 1000))

/** Shared network access for program info: one client (connections are reused) and at most 6 requests at the same time. */
object EpgNet {
    val client = Net.withTimeouts(8, 12)
    val gate = Semaphore(6)

    suspend fun fetch(url: String): String? = gate.withPermit {
        withContext(Dispatchers.IO) {
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
            } catch (e: Exception) {
                null
            }
        }
    }
}

object Xmltv {
    var status by mutableStateOf("Noch nicht geladen")
    var channelCount by mutableStateOf(0)
    private val client = Net.withTimeouts(20, 180)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    private fun prefs(c: Context) = c.getSharedPreferences("ipvio_epg", Context.MODE_PRIVATE)
    fun customUrl(c: Context): String = prefs(c).getString("custom", "") ?: ""
    fun setCustom(c: Context, u: String) { prefs(c).edit().putString("custom", u.trim()).apply() }

    /**
     * The guide shown now (T-059): shows per guide id, name matches (T-053), icons (T-055). A new read replaces it in one
     * step when it is complete, so shows and logos never disappear while the guide is read again.
     */
    @Volatile private var data = EpgData.EMPTY
    /** When the shown guide was read (0 = never) and for what (see [EpgSnapshot.sig]). */
    @Volatile private var loadedAt = 0L
    @Volatile private var loadedSig = 0L

    /** Guide id of a channel: its "tvg-id", otherwise the one found by name (null = none). */
    fun idFor(item: Item): String? = item.tvgId ?: data.byTitle[item.title]

    fun has(id: String?): Boolean = id != null && data.progs.containsKey(id.lowercase())
    fun has(item: Item): Boolean = has(idFor(item))
    fun forChannel(id: String?): List<EpgEntry> = if (id == null) emptyList() else data.progs[id.lowercase()] ?: emptyList()
    fun forItem(item: Item): List<EpgEntry> = forChannel(idFor(item))

    /** Changes when a new guide is shown, so channel lists already on screen draw new logos (read in [logo]). */
    private var iconRev by mutableStateOf(0)

    /** Logo of a channel: the playlist's own, otherwise the icon of its guide channel (null = none). */
    fun logo(item: Item): String? {
        item.logo?.let { return it }
        if (item.kind != Kind.LIVE || iconRev < 0) return null
        val d = data
        val id = item.tvgId ?: d.byTitle[item.title]
        return id?.let { d.icons[it.lowercase()] } ?: d.iconByTitle[item.title]
    }

    private fun show(d: EpgData, at: Long, sig: Long, note: String) {
        data = d; loadedAt = at; loadedSig = sig
        channelCount = d.progs.size
        iconRev++
        status = if (d.progs.isEmpty()) "Keine Programmdaten erhalten ($note)" else "Geladen: ${d.progs.size} Sender mit Programm$note"
    }

    /** Last call of [start], for the background refresh while the app runs. */
    private class Args(val c: Context, val urls: List<String>, val items: List<Item>)
    @Volatile private var last: Args? = null
    private var ticker: Job? = null

    /** While the app runs: every [EpgSnapshot.CHECK_EVERY_MS] check whether the guide is 12 hours old, then read it again. */
    private fun ensureTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(EpgSnapshot.CHECK_EVERY_MS)
                val a = last ?: continue
                if (job?.isActive != true && EpgSnapshot.refreshDue(loadedAt, System.currentTimeMillis())) start(a.c, a.urls, a.items)
            }
        }
    }

    /**
     * Loads the XMLTV program for the visible channels (hidden languages do not even arrive).
     * Only what is needed is stored: window −1 h to +36 h, at most 80 shows per channel, description shortened,
     * equal titles only once. If memory gets tight, parsing stops and keeps what has been read so far.
     *
     * T-059: the read guide is stored on the device. A stored guide for the same addresses and channels, younger than
     * 36 hours, is shown at once after a start; it is read again only when it is 12 hours old (also while the app runs),
     * when the channels change, or when [force] is set ("Reload", "Save & load"). The guide on screen stays until the
     * new one is complete.
     */
    /** [delayMs]: at app start the guide waits a little so the home screen and the playlist get the CPU first. */
    fun start(c: Context, urls: List<String>, items: List<Item>, delayMs: Long = 0L, force: Boolean = false) {
        val all = (urls + listOf(customUrl(c))).filter { it.startsWith("http") }.distinct()
        if (all.isEmpty()) { status = "Keine EPG-Adresse gefunden"; return }
        val live = items.filter { it.kind == Kind.LIVE }
        val wanted = live.mapNotNull { it.tvgId?.lowercase() }.toSet()
        // Channels without a "tvg-id" are looked up by name in the guide (T-053)
        val nameKeys = live.filter { it.tvgId == null }.map { EpgNameLogic.key(it.title) }.filter { it.isNotEmpty() }.toSet()
        if (wanted.isEmpty() && nameKeys.isEmpty()) { status = "Playlist enthält keine Sender-IDs (tvg-id)"; return }
        // Channels with archive (catchup): also keep past shows (at most 7 days, memory)
        val past = HashMap<String, Int>()
        for (it in items) {
            val id = it.tvgId ?: continue
            val d = Catchup.of(it)?.days ?: continue
            val k = id.lowercase()
            past[k] = maxOf(past[k] ?: 0, d.coerceAtMost(7))
        }
        val app = c.applicationContext
        val dir = app.cacheDir
        val snapFile = File(app.filesDir, EpgSnapshot.FILE)
        val sig = EpgSnapshot.sig(all, wanted, nameKeys, past)
        last = Args(app, urls, items)
        ensureTicker()
        job?.cancel()
        job = scope.launch {
            val now = System.currentTimeMillis()
            // 1. Stored guide for the same channels: show it at once (no reading, no download)
            if (loadedSig != sig && !force) {
                val st = runCatching { if (snapFile.exists()) java.io.FileInputStream(snapFile).use { EpgSnapshot.read(it) } else null }.getOrNull()
                if (st != null && EpgSnapshot.usable(st.savedAt, st.sig, sig, now)) show(st.data, st.savedAt, st.sig, "")
            }
            // 2. Shown guide fits and is younger than 12 hours: nothing to do
            if (!force && loadedSig == sig && !EpgSnapshot.refreshDue(loadedAt, now)) return@launch
            if (delayMs > 0) kotlinx.coroutines.delay(delayMs)
            val visibleBefore = data.progs.isNotEmpty()
            if (!visibleBefore) status = "Wird geladen …"
            val map = HashMap<String, MutableList<EpgEntry>>()
            var err: String? = null
            truncated = false
            val byName = HashMap<String, MutableSet<String>>()
            val extra = HashSet<String>()
            val icon = HashMap<String, String>()
            // Stored copies of addresses no longer in use are removed (T-055)
            runCatching { EpgCacheLogic.unused(dir.list()?.toList() ?: emptyList(), all).forEach { File(dir, it).delete() } }
            // A due refresh downloads again; a changed channel list reads the stored file (if younger than 12 hours)
            val download = force || (loadedSig == sig && EpgSnapshot.refreshDue(loadedAt, now))
            for (u in all) {
                try {
                    val f = fetch(u, File(dir, EpgCacheLogic.fileName(u)), download)
                    parse(f, wanted, map, past, nameKeys, byName, extra, icon)
                } catch (e: Exception) { err = if (e is PlaylistError) e.message else e.javaClass.simpleName }
            }
            val progs = HashMap<String, List<EpgEntry>>(map.size * 2)
            for ((k, v) in map) progs[k] = v.sortedBy { it.start }
            val byTitle = HashMap<String, String>()
            val titleIcons = HashMap<String, String>()
            for (ch in live) {
                if (ch.tvgId != null) continue
                val ids = byName[EpgNameLogic.key(ch.title)] ?: continue
                val id = EpgNameLogic.pick(ids) { g -> map[g]?.size ?: 0 }
                if (id != null) byTitle[ch.title] = id
                else ids.sorted().firstNotNullOfOrNull { icon[it] }?.let { titleIcons[ch.title] = it }
            }
            val fresh = EpgData(progs, byTitle, icon, titleIcons)
            // A failed read never replaces a shown guide of the same channels
            if (!EpgSnapshot.replace(data, fresh, sameChannels = loadedSig == sig)) {
                status = "Geladen: ${data.progs.size} Sender mit Programm (Aktualisierung fehlgeschlagen: ${err ?: "leer"})"
                return@launch
            }
            val at = System.currentTimeMillis()
            show(fresh, at, sig, if (progs.isEmpty()) (err ?: "leer") else if (truncated) " (gekürzt, Speicher knapp)" else "")
            // Keep it for the next start (written to a temporary file first, then renamed)
            if (progs.isNotEmpty()) runCatching {
                val tmp = File(snapFile.parentFile, snapFile.name + ".tmp")
                FileOutputStream(tmp).use { EpgSnapshot.write(it, fresh, at, sig) }
                snapFile.delete()
                tmp.renameTo(snapFile)
            }
        }
    }

    private fun parseTime(s: String?): Long? = xmltvTime(s)

    private const val MAX_PER_CHANNEL = 80
    private const val MAX_DESC = 400
    @Volatile private var truncated = false

    private fun shortDesc(d: String): String =
        if (d.length <= MAX_DESC) d else d.substring(0, d.lastIndexOf(' ', MAX_DESC).takeIf { it > MAX_DESC / 2 } ?: MAX_DESC) + " …"

    /**
     * [nameKeys] = name keys of channels without a "tvg-id"; the guide channels that carry such a name are collected in
     * [byName] (name key to guide ids) and their ids in [extra], so their shows are read as well. The channel list of an
     * XMLTV file comes before its shows, so [extra] is complete when the first show is read.
     */
    /**
     * The guide file of [url] as a local file (T-055): the stored copy [f] if it is younger than
     * [EpgCacheLogic.MAX_AGE_MS] and not [force], otherwise a new download. If the download fails, a stored copy up to
     * [EpgCacheLogic.STALE_MAX_MS] old is used; without one the error is passed on.
     */
    private fun fetch(url: String, f: File, force: Boolean): File {
        val age = if (f.exists() && f.length() > 0) System.currentTimeMillis() - f.lastModified() else -1L
        if (age >= 0 && EpgCacheLogic.reuse(age, force)) return f
        val tmp = File(f.parentFile, f.name + ".part")
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) throw PlaylistError("HTTP ${r.code}")
                val body = r.body ?: throw PlaylistError("leer")
                FileOutputStream(tmp).use { o -> body.byteStream().use { it.copyTo(o, 1 shl 16) } }
            }
            if (tmp.length() == 0L) throw PlaylistError("leer")
            f.delete()
            if (!tmp.renameTo(f)) throw PlaylistError("Datei")
            return f
        } catch (e: Exception) {
            tmp.delete()
            if (age >= 0 && EpgCacheLogic.fallback(age)) return f
            throw e
        }
    }

    private fun parse(
        src: File, wanted: Set<String>, out: HashMap<String, MutableList<EpgEntry>>, past: Map<String, Int> = emptyMap(),
        nameKeys: Set<String> = emptySet(), byName: HashMap<String, MutableSet<String>> = HashMap(), extra: HashSet<String> = HashSet(),
        icons: HashMap<String, String> = HashMap()
    ) {
        FileInputStream(src).use { raw ->
            var ins: InputStream = BufferedInputStream(raw)
            ins.mark(2)
            val b1 = ins.read()
            val b2 = ins.read()
            ins.reset()
            if (b1 == 0x1f && b2 == 0x8b) ins = GZIPInputStream(ins)
            val p = Xml.newPullParser()
            p.setInput(ins, null)
            val now = System.currentTimeMillis() / 1000
            val pool = StringPool()
            var seen = 0
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG && p.name == "channel") {
                    val cid = p.getAttributeValue(null, "id")?.lowercase()
                    var ic: String? = null
                    var inner = p.next()
                    while (!(inner == XmlPullParser.END_TAG && p.name == "channel") && inner != XmlPullParser.END_DOCUMENT) {
                        if (inner == XmlPullParser.START_TAG && p.name == "display-name" && cid != null && nameKeys.isNotEmpty()) {
                            val k = EpgNameLogic.key(p.nextText())
                            if (k in nameKeys) { byName.getOrPut(k) { HashSet() }.add(cid); extra.add(cid) }
                        } else if (inner == XmlPullParser.START_TAG && p.name == "icon" && ic == null) {
                            ic = EpgCacheLogic.iconUrl(p.getAttributeValue(null, "src"))
                        }
                        inner = p.next()
                    }
                    // Icon only for channels of this playlist (by tvg-id or by name), so memory stays small
                    if (cid != null && ic != null && (cid in wanted || cid in extra)) icons[cid] = ic
                } else if (ev == XmlPullParser.START_TAG && p.name == "programme") {
                    val ch = p.getAttributeValue(null, "channel")?.lowercase()
                    val s = parseTime(p.getAttributeValue(null, "start"))
                    val e = parseTime(p.getAttributeValue(null, "stop"))
                    var title = ""
                    var desc = ""
                    var inner = p.next()
                    while (!(inner == XmlPullParser.END_TAG && p.name == "programme") && inner != XmlPullParser.END_DOCUMENT) {
                        if (inner == XmlPullParser.START_TAG) {
                            if (p.name == "title" && title.isEmpty()) title = p.nextText()
                            else if (p.name == "desc" && desc.isEmpty()) desc = p.nextText()
                        }
                        inner = p.next()
                    }
                    val pd = if (ch != null) past[ch] ?: 0 else 0
                    if (ch != null && (ch in wanted || ch in extra) && s != null && e != null &&
                        e > now - (if (pd > 0) pd * 86400L else 3600L) && s < now + 129600 && epgSane(s, e)
                    ) {
                        val l = out.getOrPut(ch) { ArrayList() }
                        if (l.size < MAX_PER_CHANNEL + pd * 48) l.add(EpgEntry(pool.of(title), shortDesc(desc), s, e))
                    }
                    // Memory is getting tight: stop and keep what has been read so far (better gaps in the program than a crash)
                    if (++seen % 5000 == 0 && Mem.heapTight(0.70)) { truncated = true; return }
                }
                ev = p.next()
            }
        }
    }
}

/** Small cache with an upper limit: the most recently used entries stay, the oldest drop out. */
class LruCache<K, V>(private val max: Int) {
    private val map = object : LinkedHashMap<K, V>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?) = size > max
    }
    @Synchronized operator fun get(k: K): V? = map[k]
    @Synchronized operator fun set(k: K, v: V) { map[k] = v }
    @Synchronized fun clear() = map.clear()
}

object Epg {
    private val cache = LruCache<String, Pair<Long, List<EpgEntry>>>(400)
    fun trim() = cache.clear()
    private val LIVE_RE = Regex("""^(https?://[^/]+)/(?:live/)?([^/]+)/([^/]+)/(\d+)(?:\.\w+)?(?:\?.*)?$""")

    fun supported(item: Item): Boolean = LIVE_RE.matches(item.url) || Xmltv.has(item)

    /** Address of the Xtream program query [action] for a channel or null if the channel has no Xtream address. */
    internal fun xtreamApi(item: Item, action: String): String? {
        val p = LIVE_RE.find(item.url)?.groupValues ?: return null
        return "${p[1]}/player_api.php?username=${p[2]}&password=${p[3]}&action=$action&stream_id=${p[4]}"
    }

    /** Xtream delivers title and description Base64-encoded, but some providers in plain text. */
    private fun decode(s: String): String {
        if (s.isBlank()) return ""
        return try {
            val d = String(Base64.decode(s, Base64.DEFAULT), Charsets.UTF_8)
            if (d.isBlank() || d.contains('\uFFFD')) s else d
        } catch (e: Exception) {
            s
        }
    }

    /** Reads "epg_listings" of an Xtream response; broken entries are skipped. Result sorted by start time. */
    internal fun parseListings(body: String, keep: (start: Long, end: Long) -> Boolean = { _, _ -> true }): List<EpgEntry> {
        val out = ArrayList<EpgEntry>()
        try {
            val a = JSONObject(body).optJSONArray("epg_listings")
            if (a != null) for (x in 0 until a.length()) {
                val o = a.getJSONObject(x)
                val s = o.optString("start_timestamp").toLongOrNull() ?: continue
                val e = o.optString("stop_timestamp").toLongOrNull() ?: continue
                if (!epgSane(s, e) || !keep(s, e)) continue
                out.add(EpgEntry(decode(o.optString("title")), decode(o.optString("description")), s, e))
            }
        } catch (e: Exception) {
        }
        out.sortBy { it.start }
        return out
    }

    /** Running and upcoming shows of a channel. Order: cache, loaded XMLTV (immediately), then Xtream query. */
    suspend fun now(item: Item): List<EpgEntry> {
        val nowMs = System.currentTimeMillis()
        val nowSec = nowMs / 1000
        cache[item.id]?.let { if (nowMs - it.first < 120_000) return it.second }
        // Everything below (XMLTV filter, JSON parsing, Base64) runs off the main thread, whoever calls this
        return withContext(Dispatchers.Default) { nowFresh(item, nowMs, nowSec) }
    }

    private suspend fun nowFresh(item: Item, nowMs: Long, nowSec: Long): List<EpgEntry> {
        val local = Xmltv.forItem(item).filter { it.end > nowSec }
        if (local.isNotEmpty() && current(local) != null) return local.take(8).also { cache[item.id] = nowMs to it }
        val url = xtreamApi(item, "get_short_epg")
        if (url != null) {
            val body = EpgNet.fetch("$url&limit=8")
            if (body != null) {
                val out = parseListings(body)
                if (out.isNotEmpty()) { cache[item.id] = nowMs to out; return out }
            }
        }
        return local.take(8)
    }

    /** Cached result of [now] if it is still fresh, without any lookup or network (cheap enough for composition). */
    fun peek(item: Item): List<EpgEntry>? {
        val c = cache[item.id] ?: return null
        return if (System.currentTimeMillis() - c.first < 120_000) c.second else null
    }

    fun current(list: List<EpgEntry>): EpgEntry? {
        val n = System.currentTimeMillis() / 1000
        return list.firstOrNull { n >= it.start && n < it.end }
    }
}

data class Seg(val label: String, val start: Long, val end: Long)

object IntroDb {
    private val client = Net.base

    suspend fun get(tmdbId: Int, season: Int, episode: Int, durMs: Long): List<Seg> {
        val tv = if (season > 0 && episode > 0) "&season=$season&episode=$episode" else ""
        val url = "https://api.theintrodb.org/v3/media?tmdb_id=$tmdbId$tv&duration_ms=$durMs"
        val body = withContext(Dispatchers.IO) {
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                    if (r.isSuccessful) r.body?.string() else null
                }
            } catch (e: Exception) {
                null
            }
        } ?: return emptyList()
        val out = ArrayList<Seg>()
        try {
            val o = JSONObject(body)
            // Only plausible sections (see SegLogic): missing end times for intro/recap or an "intro" in the middle of the episode
            // would otherwise lead to buttons that do not fit
            fun read(key: String, kind: Int, label: String) {
                val a = o.optJSONArray(key) ?: return
                for (x in 0 until a.length()) {
                    val s = a.optJSONObject(x) ?: continue
                    val st = if (!s.has("start_ms") || s.isNull("start_ms")) null else s.optLong("start_ms")
                    val en = if (!s.has("end_ms") || s.isNull("end_ms")) null else s.optLong("end_ms")
                    SegLogic.clean(kind, st, en, durMs)?.let { (a0, b0) -> out.add(Seg(label, a0, b0)) }
                }
            }
            read("intro", SegLogic.INTRO, "Intro überspringen")
            read("recap", SegLogic.RECAP, "Rückblick überspringen")
            read("credits", SegLogic.CREDITS, "Abspann überspringen")
        } catch (e: Exception) {
        }
        return out
    }
}
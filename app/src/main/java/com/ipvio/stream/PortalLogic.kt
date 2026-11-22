package com.ipvio.stream

/**
 * Key-based video portals ("vportal", T-036). Without Android, testable.
 *
 * The user enters (or pastes into the playlist address field) one text of the form
 * `portal::[key:<KEY>]http://<host>/api/v1/`. There is no MAC address and no user name: every request is a JSON POST to
 * that address and carries the key. The list is built from the portal's categories; the app writes it as M3U text
 * like it does for Stalker portals (see [PortalApi]).
 *
 * - A movie ("stream") already has its HLS address. It is written with the marker `#/movie/` behind the address: the
 *   marker is a URL fragment (never sent to the server, ignored by the player) that only makes the list parser classify
 *   the entry as a movie ([Models] looks for "/movie/").
 * - A series ("multistream") appears with a placeholder `portal://SOURCE/series/FID.ser` and the name "… S01E01", just like
 *   Stalker series. Its episodes come from [PortalApi.episodeList] when the series is opened ([LazySeries]).
 */
object PortalLogic {
    /** Login: the portal's API address and the key. */
    data class Source(val base: String, val key: String) {
        /** Id of the source in placeholder addresses (hash of the stored text, never the key itself). */
        val id: Int get() = encode().hashCode()

        /** Stored form = the text as the user pasted it (normalized). */
        fun encode(): String = "portal::[key:$key]$base"
    }

    private val INPUT = Regex("""^portal::\[key:([A-Za-z0-9._-]{6,128})\](https?://[^\s/?#"'<>]+(?:/[^\s"'<>]*)?)$""", RegexOption.IGNORE_CASE)

    fun isSource(url: String): Boolean = url.trim().startsWith("portal::[key:", ignoreCase = true)

    /** Login from the pasted text; null = not a valid portal text (the base address gets a trailing "/"). */
    fun parse(raw: String): Source? {
        val m = INPUT.matchEntire(raw.trim()) ?: return null
        var base = m.groupValues[2]
        if (!base.endsWith("/")) base += "/"
        return Source(base, m.groupValues[1])
    }

    /** Category of the portal: [title] (as the portal names it) and the request to list it (always with "cmd" and "fid"). */
    class Cat(val title: String, val fid: Int, val request: Map<String, Any>)

    /** "Continue watching" depends on the account's history, not on the catalogue: not listed. */
    private const val CONTINUE_WATCHING = 10001

    /** Category from the entry answer; null = not a usable category. */
    fun cat(title: String, request: Map<String, Any>): Cat? {
        val t = title.trim().ifEmpty { return null }
        if (request["cmd"] != "flicks") return null
        val fid = (request["fid"] as? Number)?.toInt() ?: return null
        if (fid == CONTINUE_WATCHING) return null
        return Cat(t, fid, request)
    }

    /** Request for the page that starts at [offset] (the portal names the offset in its "next" entry). */
    fun nextRequest(base: Map<String, Any>, offset: Int): Map<String, Any> = LinkedHashMap(base).also { it["offset"] = offset }

    /** Title as the portal sends it: [type] "stream" (movie) or "multistream" (series). */
    class Entry(
        val type: String, val title: String, val img: String, val fid: Int, val url: String, val year: Int, val description: String = ""
    )

    /** One page of a category: its titles and the offset of the next page (null = last page). */
    class Page(val entries: List<Entry>, val next: Int?, val count: Int)

    /**
     * Offset for the next page, or null at the end. A page is also the last one if the offset does not move forward
     * ([last] = the offset of the page just loaded), so a portal that repeats itself cannot cause an endless loop.
     */
    fun nextOffset(next: Int?, last: Int): Int? = next?.takeIf { it > last }

    private fun attr(s: String?) = (s ?: "").replace('"', '\'').replace('\n', ' ').replace('\r', ' ').trim()
    private fun name(s: String?) = (s ?: "").replace(',', '‚').replace('\n', ' ').replace('\r', ' ').trim()
    private fun isHttp(u: String) = u.startsWith("http://", true) || u.startsWith("https://", true)

    /** Marker behind the address of a movie, see the class text. */
    const val MOVIE_MARK = "#/movie/"

    /** Address of a movie as the list holds it: the HLS address plus [MOVIE_MARK]. */
    fun movieUrl(url: String): String = url.substringBefore('#') + MOVIE_MARK

    /** A movie as two M3U lines; null = no usable address or title. */
    fun movieEntry(e: Entry, group: String): String? {
        if (e.type != "stream" || !isHttp(e.url) || e.url.any { it.isWhitespace() }) return null
        var n = name(e.title).ifEmpty { return null }
        if (e.year in 1850..2100 && !n.contains("(${e.year})")) n = "$n (${e.year})"
        val sb = StringBuilder("#EXTINF:-1")
        e.img.trim().takeIf { isHttp(it) }?.let { sb.append(" tvg-logo=\"").append(attr(it)).append('"') }
        sb.append(" group-title=\"").append(attr(group).ifEmpty { "Filme" }).append('"')
        return sb.append(',').append(n).append('\n').append(movieUrl(e.url)).toString()
    }

    /** A series as a placeholder (two M3U lines); the name ends in "S01E01", so parsing recognizes a series. */
    fun seriesEntry(src: Source, e: Entry, group: String): String? {
        if (e.type != "multistream" || e.fid <= 0) return null
        val n = name(e.title).ifEmpty { return null }
        val sb = StringBuilder("#EXTINF:-1")
        e.img.trim().takeIf { isHttp(it) }?.let { sb.append(" tvg-logo=\"").append(attr(it)).append('"') }
        sb.append(" group-title=\"").append(attr(group).ifEmpty { "Serien" }).append('"')
        return sb.append(',').append(n).append(" S01E01").append('\n').append(seriesUrl(src.id, e.fid)).toString()
    }

    /**
     * Categories whose "series" entries are often single films: the portal lists a film with one file as a series with
     * one episode (e.g. a film in the movies category opened as "1 season, 1 episode"). Ids: 1 = movies, 3 = cartoons, 5 = 3D.
     */
    val FILM_CATEGORIES = setOf(1, 3, 5)

    /**
     * A "series" entry that has exactly one playable file is a film: returns it as a movie entry with that file's address.
     * null = it is a real series (several files, none, or the answer lists further series).
     */
    fun singleAsMovie(e: Entry, items: List<Entry>): Entry? {
        if (e.type != "multistream" || items.any { it.type == "multistream" }) return null
        val files = items.filter { it.type == "stream" && isHttp(it.url) && it.url.none { c -> c.isWhitespace() } }
        if (files.size != 1) return null
        return Entry("stream", e.title, e.img, e.fid, files[0].url.substringBefore('#'), e.year, e.description)
    }

    // ------------------------------------------------------------------ Series placeholder

    fun seriesUrl(sourceId: Int, fid: Int): String = "portal://$sourceId/series/$fid.ser"

    class SeriesRef(val sourceId: Int, val fid: Int)

    fun seriesRef(url: String): SeriesRef? {
        if (!url.startsWith("portal://")) return null
        val rest = url.removePrefix("portal://").split('/')
        if (rest.size != 3 || rest[1] != "series" || !rest[2].endsWith(".ser")) return null
        val id = rest[0].toIntOrNull() ?: return null
        val fid = rest[2].removeSuffix(".ser").toIntOrNull()?.takeIf { it > 0 } ?: return null
        return SeriesRef(id, fid)
    }

    /** Placeholder addresses of a series (several playlists shown at once give several). Empty = real episodes. */
    fun lazyUrls(i: Item): List<Pair<String, String>> {
        if (i.kind != Kind.SERIES || i.episodes.size != 1) return emptyList()
        val e = i.episodes[0]
        val vs = e.variants.ifEmpty { listOf(Variant(e.url, "", e.quality, null)) }
        if (vs.any { seriesRef(it.url) == null }) return emptyList()
        return vs.map { it.url to it.label }
    }

    fun isLazy(i: Item): Boolean = lazyUrls(i).isNotEmpty()

    /**
     * Episodes of a series from its list entries: one season, numbered in the order the portal sends them.
     * Entries without a usable address are skipped; a missing title becomes "Episode N".
     */
    fun episodes(entries: List<Entry>): List<Episode> {
        val out = ArrayList<Episode>()
        for (e in entries) {
            if (e.type != "stream" || !isHttp(e.url) || e.url.any { it.isWhitespace() }) continue
            val num = out.size + 1
            out.add(Episode(1, num, e.title.trim().ifEmpty { EpTitles.of(num) }, e.url.substringBefore('#')))
        }
        return out
    }
}

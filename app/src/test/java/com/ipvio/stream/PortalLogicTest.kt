package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Key-based video portal (T-051): input text, categories, paging, list entries, series placeholder, episodes. Synthetic data only. */
class PortalLogicTest {
    private val key = "aaaabbbbccccdddd-0123456789abcde"
    private val base = "http://portal.example.test/api/v1/"
    private val src = PortalLogic.Source(base, key)

    private fun entry(type: String, title: String, fid: Int, url: String = "", year: Int = 0, img: String = "") =
        PortalLogic.Entry(type, title, img, fid, url, year)

    @Test fun parsesThePastedText() {
        val s = PortalLogic.parse("  portal::[key:$key]$base ")
        assertEquals(PortalLogic.Source(base, key), s)
        assertEquals("portal::[key:$key]$base", s!!.encode())
    }

    @Test fun addsTheTrailingSlash() {
        assertEquals("http://h.example.test:8080/api/v1/", PortalLogic.parse("portal::[key:$key]http://h.example.test:8080/api/v1")?.base)
    }

    @Test fun rejectsGarbage() {
        assertNull(PortalLogic.parse(""))
        assertNull(PortalLogic.parse("http://portal.example.test/api/v1/"))
        assertNull(PortalLogic.parse("portal::[key:]$base"))
        assertNull(PortalLogic.parse("portal::[key:short]$base"))
        assertNull(PortalLogic.parse("portal::[key:$key]ftp://portal.example.test/"))
        assertNull(PortalLogic.parse("portal::[key:$key]http://"))
        assertNull(PortalLogic.parse("portal::[key:$key]http://a b.example.test/"))
        assertNull(PortalLogic.parse("portal::[key:ke y-with-space]$base"))
    }

    @Test fun recognizesTheSourceByItsStart() {
        assertTrue(PortalLogic.isSource(" PORTAL::[key:abc"))
        assertFalse(PortalLogic.isSource("stalker:http://x#mac=00:1A:79:00:00:01"))
        assertFalse(PortalLogic.isSource("http://x/get.php"))
    }

    @Test fun sourceIdDoesNotContainTheKey() {
        assertFalse(src.id.toString().contains(key))
        assertEquals(src.id, PortalLogic.parse(src.encode())!!.id)
        assertTrue(src.id != PortalLogic.Source(base, "other-key-0001").id)
    }

    @Test fun playlistLogicAcceptsThePortalText() {
        assertEquals("portal::[key:$key]$base", PlaylistLogic.normalizeUrl("portal::[key:$key]$base"))
        assertNull(PlaylistLogic.normalizeUrl("portal::[key:$key]nothing"))
    }

    @Test fun categoryKeepsItsRequestAndSkipsContinueWatching() {
        val req = mapOf<String, Any>("cmd" to "flicks", "fid" to 1, "offset" to 0, "limit" to 0)
        val c = PortalLogic.cat(" Movies ", req)!!
        assertEquals("Movies", c.title)
        assertEquals(1, c.fid)
        assertEquals(req, c.request)
        assertNull(PortalLogic.cat("Continue", mapOf("cmd" to "flicks", "fid" to 10001)))
        assertNull(PortalLogic.cat("", req))
        assertNull(PortalLogic.cat("X", mapOf("cmd" to "flick", "fid" to 5)))
        assertNull(PortalLogic.cat("X", mapOf("cmd" to "flicks")))
    }

    @Test fun nextRequestOnlyChangesTheOffset() {
        val req = mapOf<String, Any>("cmd" to "flicks", "fid" to 2, "offset" to 0, "limit" to 0, "vc" to 1)
        val n = PortalLogic.nextRequest(req, 30)
        assertEquals(30, n["offset"])
        assertEquals(2, n["fid"])
        assertEquals(1, n["vc"])
        assertEquals(0, req["offset"])
    }

    @Test fun pagingStopsWhenTheOffsetDoesNotMoveForward() {
        assertEquals(30, PortalLogic.nextOffset(30, 0))
        assertEquals(60, PortalLogic.nextOffset(60, 30))
        assertNull(PortalLogic.nextOffset(null, 30))
        assertNull(PortalLogic.nextOffset(30, 30))
        assertNull(PortalLogic.nextOffset(0, 30))
    }

    @Test fun movieEntryIsClassifiedAsMovieAndKeepsTheStreamAddress() {
        val url = "http://cdn.example.test/vod/k/S30/00025415/0/master.m3u8"
        val m = PortalLogic.movieEntry(entry("stream", "Title, One", 7, url, 2026, "http://portal.example.test/posters/h/p_1.jpg"), "Movies")!!
        val lines = m.lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("#EXTINF:-1 portal-fid=\"7\" tvg-logo=\"http://portal.example.test/posters/h/p_1.jpg\" group-title=\"Movies\","))
        assertTrue(lines[0].endsWith("Title‚ One (2026)"))
        assertEquals(url + "#/movie/", lines[1])
        assertTrue(lines[1].contains("/movie/"))
        // never sent to the server: the part before the fragment is the stream address
        assertEquals(url, lines[1].substringBefore('#'))
    }

    @Test fun movieEntryRejectsBadInput() {
        assertNull(PortalLogic.movieEntry(entry("multistream", "A", 1, "http://x.example.test/a.m3u8"), "G"))
        assertNull(PortalLogic.movieEntry(entry("stream", "A", 1, ""), "G"))
        assertNull(PortalLogic.movieEntry(entry("stream", "A", 1, "javascript:alert(1)"), "G"))
        assertNull(PortalLogic.movieEntry(entry("stream", "A", 1, "http://x.example.test/a b.m3u8"), "G"))
        assertNull(PortalLogic.movieEntry(entry("stream", "  ", 1, "http://x.example.test/a.m3u8"), "G"))
    }

    @Test fun movieTextCannotBreakTheLine() {
        val m = PortalLogic.movieEntry(entry("stream", "A\nB", 1, "http://x.example.test/a.m3u8", 0, "http://x.example.test/p\".jpg"), "G\"r\nup")!!
        assertEquals(2, m.lines().size)
        assertTrue(m.lines()[0].contains("group-title=\"G'r up\""))
        assertTrue(m.lines()[0].contains("tvg-logo=\"http://x.example.test/p'.jpg\""))
    }

    @Test fun movieUrlMarkIsAddedOnlyOnce() {
        val u = "http://x.example.test/a.m3u8"
        assertEquals(PortalLogic.movieUrl(u), PortalLogic.movieUrl(PortalLogic.movieUrl(u)))
    }

    @Test fun seriesEntryIsAPlaceholderThatParsingReadsAsSeries() {
        val s = PortalLogic.seriesEntry(src, entry("multistream", "Show", 11405), "Series")!!
        val lines = s.lines()
        assertTrue(lines[0].endsWith(",Show S01E01"))
        assertTrue(lines[1].contains("/series/"))
        val ref = PortalLogic.seriesRef(lines[1])!!
        assertEquals(src.id, ref.sourceId)
        assertEquals(11405, ref.fid)
        assertNull(PortalLogic.seriesEntry(src, entry("multistream", "Show", 0), "Series"))
        assertNull(PortalLogic.seriesEntry(src, entry("stream", "Show", 5, "http://x.example.test/a.m3u8"), "Series"))
    }

    @Test fun seriesRefRejectsGarbage() {
        assertNull(PortalLogic.seriesRef("http://x.example.test/series/5.ser"))
        assertNull(PortalLogic.seriesRef("portal://abc/series/5.ser"))
        assertNull(PortalLogic.seriesRef("portal://12/series/0.ser"))
        assertNull(PortalLogic.seriesRef("portal://12/movie/5.ser"))
        assertNull(PortalLogic.seriesRef("portal://12/series/5"))
        assertNotNull(PortalLogic.seriesRef("portal://-12/series/5.ser"))
    }

    @Test fun episodesAreNumberedInPortalOrderAndKeepTheirAddress() {
        val l = PortalLogic.episodes(listOf(
            entry("stream", "Episode A", 1, "http://c.example.test/0/master.m3u8"),
            entry("stream", "", 2, "http://c.example.test/1/master.m3u8"),
            entry("stream", "No address", 3, ""),
            entry("category", "Skipped", 4, "http://c.example.test/x.m3u8"),
            entry("stream", "Episode D", 5, "http://c.example.test/3/master.m3u8#/movie/"),
        ))
        assertEquals(listOf(1, 2, 3), l.map { it.num })
        assertEquals(listOf(1, 1, 1), l.map { it.season })
        assertEquals(listOf("Episode A", "Episode 2", "Episode D"), l.map { it.title })
        assertEquals("http://c.example.test/3/master.m3u8", l[2].url)
    }

    @Test fun anEmptyEpisodeListStaysEmpty() {
        assertTrue(PortalLogic.episodes(emptyList()).isEmpty())
    }

    @Test fun tapLogicIgnoresTheMovieMark() {
        // A portal movie is an HLS playlist, not a continuous stream: no live buffer or recording for it
        assertFalse(TapLogic.tappable(PortalLogic.movieUrl("http://c.example.test/vod/k/0/master.m3u8")))
        assertTrue(TapLogic.tappable("http://c.example.test/live/1.ts"))
    }
    @Test fun aSeriesEntryWithOneFileIsAFilm() {
        val e = PortalLogic.Entry("multistream", "Film", "http://p.example.test/i.jpg", 9, "", 2007, "text")
        val one = listOf(entry("stream", "Series 1", 90, "http://c.example.test/0/master.m3u8"))
        val m = PortalLogic.singleAsMovie(e, one)!!
        assertEquals("stream", m.type)
        assertEquals("Film", m.title)
        assertEquals(9, m.fid)
        assertEquals(2007, m.year)
        assertEquals("http://c.example.test/0/master.m3u8", m.url)
        // and it is filed as a movie afterwards
        assertTrue(PortalLogic.movieEntry(m, "Movies")!!.lines()[1].contains("/movie/"))
    }

    @Test fun realSeriesStaySeries() {
        val e = PortalLogic.Entry("multistream", "Show", "", 9, "", 2000)
        val two = listOf(entry("stream", "S1", 1, "http://c.example.test/0/m.m3u8"), entry("stream", "S2", 2, "http://c.example.test/1/m.m3u8"))
        assertNull(PortalLogic.singleAsMovie(e, two))
        assertNull(PortalLogic.singleAsMovie(e, emptyList()))
        assertNull(PortalLogic.singleAsMovie(e, listOf(entry("stream", "S1", 1, ""))))
        assertNull(PortalLogic.singleAsMovie(e, listOf(entry("multistream", "Part", 3))))
        // a plain movie is not touched
        assertNull(PortalLogic.singleAsMovie(PortalLogic.Entry("stream", "M", "", 1, "http://c.example.test/a.m3u8", 0), two))
    }

    @Test fun onlyFilmCategoriesAreChecked() {
        assertTrue(1 in PortalLogic.FILM_CATEGORIES)
        assertFalse(2 in PortalLogic.FILM_CATEGORIES)
        assertFalse(11 in PortalLogic.FILM_CATEGORIES)
    }

    @Test fun aSeriesInAFilmCategoryGoesToTheSeriesGroup() {
        val films = PortalLogic.cat("movies", mapOf("cmd" to "flicks", "fid" to 1))!!
        val tv = PortalLogic.cat("shows", mapOf("cmd" to "flicks", "fid" to 2))!!
        val series = entry("multistream", "Show", 5)
        val film = entry("stream", "Film", 6, "http://c.example.test/a.m3u8")
        assertEquals("shows", PortalLogic.groupFor(series, films, "shows"))
        assertEquals("movies", PortalLogic.groupFor(film, films, "shows"))
        assertEquals("shows", PortalLogic.groupFor(series, tv, "shows"))
        assertEquals("movies", PortalLogic.groupFor(series, films, null))
        assertEquals("movies", PortalLogic.groupFor(series, films, " "))
    }

    // ---------------------------------------------------------------- Update with new titles only (T-054)

    @Test fun entriesCarryTheTitleIdSoAnUpdateKnowsThem() {
        val src = PortalLogic.Source("http://portal.example.test/api/v1/", "abcdef123")
        val m = PortalLogic.movieEntry(entry("stream", "Film", 148716, "http://cdn.example.test/vod/k/a.m3u8"), "Movies")!!
        val s = PortalLogic.seriesEntry(src, entry("multistream", "Serie", 144916), "Series")!!
        assertEquals(148716, PortalLogic.fidOf(m.lines()[0]))
        assertEquals(144916, PortalLogic.fidOf(s.lines()[0]))
        assertNull(PortalLogic.fidOf("#EXTINF:-1 group-title=\"x\",Old entry without id"))
    }

    @Test fun headLineRoundTrips() {
        val h = PortalLogic.parseHead(PortalLogic.head(1_700_000_000_000L, "cdn.example.test"))!!
        assertEquals(1_700_000_000_000L, h.fullAt)
        assertEquals("cdn.example.test", h.vodHost)
        assertNull(PortalLogic.parseHead(PortalLogic.head(5L, null))!!.vodHost)
        assertNull(PortalLogic.parseHead("#PORTAL-LIST:3 full=5 vod=x"))
        assertNull(PortalLogic.parseHead("#EXTINF:-1,Name"))
    }

    @Test fun fullLoadAfterThirtyDaysOrWithoutStoredList() {
        val day = 24 * 3_600_000L
        val now = 100 * day
        assertTrue(PortalLogic.needsFull(null, now))
        assertFalse(PortalLogic.needsFull(now - 29 * day, now))
        assertTrue(PortalLogic.needsFull(now - 30 * day, now))
        assertTrue(PortalLogic.needsFull(now + day, now))
    }

    @Test fun updateStopsAtTheFirstPageWithOnlyKnownTitles() {
        val known = setOf(1, 2, 3)
        assertTrue(PortalLogic.pageKnown(listOf(entry("stream", "a", 1), entry("stream", "b", 2)), known))
        assertFalse(PortalLogic.pageKnown(listOf(entry("stream", "new", 9), entry("stream", "b", 2)), known))
        assertFalse(PortalLogic.pageKnown(emptyList(), known))
    }

    @Test fun listStartAndHeadAreNotCopied() {
        assertFalse(PortalLogic.keepLine("#EXTM3U"))
        assertFalse(PortalLogic.keepLine(PortalLogic.head(1L, "h")))
        assertFalse(PortalLogic.keepLine("   "))
        assertTrue(PortalLogic.keepLine("#EXTINF:-1 portal-fid=\"1\",x"))
        assertTrue(PortalLogic.keepLine("http://cdn.example.test/vod/a.m3u8#/movie/"))
    }

    @Test fun hostOfAMovieAddress() {
        assertEquals("165474e569b7.usanetworkss.com", PortalLogic.hostOf("http://165474E569B7.usanetworkss.com/vod/k/S30/1/0/master.m3u8"))
        assertNull(PortalLogic.hostOf("portal://1/series/2.ser"))
    }

    @Test fun fullLoadOnlyWhenTheStoredMovieHostIsGone() {
        assertTrue(PortalLogic.hostGone("a.test", setOf("b.test")))
        assertFalse(PortalLogic.hostGone("a.test", setOf("a.test", "b.test")))
        assertFalse(PortalLogic.hostGone("a.test", emptySet()))
        assertFalse(PortalLogic.hostGone(null, setOf("b.test")))
    }
}

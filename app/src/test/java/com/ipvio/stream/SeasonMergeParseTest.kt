package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** #151: the parser joins seasons that an Xtream provider lists as separate series, only when asked (pure, no Android). */
class SeasonMergeParseTest {

    private val list = """
        #EXTM3U
        #EXTINF:-1 group-title="Serien",Show S01 S01E01
        http://s.tv:80/series/u/p/11.xtser
        #EXTINF:-1 group-title="Serien",Show S02 S01E01
        http://s.tv:80/series/u/p/12.xtser
        #EXTINF:-1 group-title="Serien",Other Show S01E01
        http://s.tv:80/series/u/p/13.xtser
    """.trimIndent()

    @Test fun off_keepsEverySeasonAsItsOwnSeries_asBefore() {
        val s = M3uParser.parseLines(list.lineSequence()).items.filter { it.kind == Kind.SERIES }
        assertEquals(listOf("s_show s01", "s_show s02", "s_other show"), s.map { it.id })
    }

    @Test fun on_joinsTheSeasonsOfOneShow() {
        val s = M3uParser.parseLines(list.lineSequence(), setOf(LangTagLogic.MERGE_SEASONS)).items.filter { it.kind == Kind.SERIES }
        assertEquals(listOf("s_show", "s_other show"), s.map { it.id })
        val show = s.first()
        assertEquals("Show", show.title)
        assertEquals(listOf(1 to 1, 2 to 1), show.episodes.map { it.season to it.num })
        // Both placeholders are found for loading, each with its season
        assertEquals(listOf(1, 2), XtreamLogic.lazyParts(show).map { it.third })
        assertEquals(listOf("http://s.tv:80/series/u/p/11.xtser", "http://s.tv:80/series/u/p/12.xtser"), XtreamLogic.lazyParts(show).map { it.first })
    }

    @Test fun on_doesNotTouchSeriesWithoutAMarkerOrNormalEpisodes() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 group-title="Serien",Plain Show S01E01
            http://s.tv/series/u/p/1.mkv
            #EXTINF:-1 group-title="Serien",Plain Show S01E02
            http://s.tv/series/u/p/2.mkv
            #EXTINF:-1 group-title="Serien",Show S02 S01E01
            http://s.tv/series/u/p/3.mkv
        """.trimIndent()
        val s = M3uParser.parseLines(m3u.lineSequence(), setOf(LangTagLogic.MERGE_SEASONS)).items.filter { it.kind == Kind.SERIES }
        // Real files: no change, the title "Show S02" stays as it is
        assertEquals(listOf("s_plain show", "s_show s02"), s.map { it.id })
        assertEquals(2, s.first().episodes.size)
        assertEquals(emptyList<Triple<String, String, Int>>(), XtreamLogic.lazyParts(s.first()))
    }

    // T-075: the video portal lists a series once per season, with "Сезон N" in the name
    private val portal = """
        #EXTM3U
        #EXTINF:-1 portal-fid="105740" group-title="сериалы",Люпен. Сезон 1. S01E01
        portal://7/series/105740.ser
        #EXTINF:-1 portal-fid="68223" group-title="сериалы",Люпен S01E01
        portal://7/series/68223.ser
        #EXTINF:-1 portal-fid="105726" group-title="сериалы",Люпен. Сезон 2. S01E01
        portal://7/series/105726.ser
        #EXTINF:-1 portal-fid="93175" group-title="сериалы",Тверская S01E01
        portal://7/series/93175.ser
    """.trimIndent()

    @Test fun portal_off_keepsEverySeasonAsItsOwnSeries() {
        val s = M3uParser.parseLines(portal.lineSequence()).items.filter { it.kind == Kind.SERIES }
        assertEquals(4, s.size)
        assertEquals(listOf("portal://7/series/105740.ser"), PortalLogic.lazyUrls(s.first()).map { it.first })
    }

    @Test fun portal_on_joinsTheSeasonsAndLoadsEachWithItsSeason() {
        val s = M3uParser.parseLines(portal.lineSequence(), setOf(LangTagLogic.MERGE_SEASONS)).items.filter { it.kind == Kind.SERIES }
        assertEquals(listOf("Люпен", "Тверская"), s.map { it.title })
        val lupin = s.first()
        // "Люпен" without a season and "Люпен. Сезон 1." are two sources of season 1
        assertEquals(listOf(1, 1, 2), PortalLogic.lazyParts(lupin).map { it.third })
        assertEquals(
            setOf("portal://7/series/105740.ser", "portal://7/series/68223.ser", "portal://7/series/105726.ser"),
            PortalLogic.lazyParts(lupin).map { it.first }.toSet()
        )
        assertEquals(listOf(1), PortalLogic.lazyParts(s[1]).map { it.third })
    }
}

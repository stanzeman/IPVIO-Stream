package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Stored guide files and channel icons (T-055). */
class EpgCacheLogicTest {
    private val hour = 3_600_000L

    @Test
    fun youngCopyIsReused() {
        assertTrue(EpgCacheLogic.reuse(0, force = false))
        assertTrue(EpgCacheLogic.reuse(11 * hour, force = false))
    }

    @Test
    fun oldCopyOrForcedReloadDownloadsAgain() {
        assertFalse(EpgCacheLogic.reuse(12 * hour, force = false))
        assertFalse(EpgCacheLogic.reuse(1 * hour, force = true))
    }

    @Test
    fun clockGoingBackDoesNotKeepACopyForever() {
        assertFalse(EpgCacheLogic.reuse(-5, force = false))
        assertFalse(EpgCacheLogic.fallback(-5))
    }

    @Test
    fun failedDownloadFallsBackToACopyUpToSevenDays() {
        assertTrue(EpgCacheLogic.fallback(6 * 24 * hour))
        assertFalse(EpgCacheLogic.fallback(7 * 24 * hour))
    }

    @Test
    fun fileNameDependsOnTheAddressOnly() {
        val a = EpgCacheLogic.fileName("http://epg.one/epg2.xml.gz")
        assertEquals(a, EpgCacheLogic.fileName("  http://epg.one/epg2.xml.gz "))
        assertTrue(a.startsWith("epg_") && a.endsWith(".xml"))
        assertFalse(a.contains("epg.one"))
        assertFalse(a == EpgCacheLogic.fileName("http://epg.one/epg.xml.gz"))
    }

    @Test
    fun copiesOfAddressesNoLongerUsedAreRemoved() {
        val used = EpgCacheLogic.fileName("http://a/x.xml")
        val old = EpgCacheLogic.fileName("http://b/y.xml")
        val names = listOf(used, old, "other.bin", "epg_part.xml.part")
        assertEquals(listOf(old), EpgCacheLogic.unused(names, listOf("http://a/x.xml")))
    }

    @Test
    fun onlyWebAddressesAreUsedAsIcons() {
        assertEquals("http://epg.one/img2/8900.png", EpgCacheLogic.iconUrl(" http://epg.one/img2/8900.png "))
        assertNull(EpgCacheLogic.iconUrl("file:///etc/passwd"))
        assertNull(EpgCacheLogic.iconUrl("http://a b/c.png"))
        assertNull(EpgCacheLogic.iconUrl(null))
    }
}

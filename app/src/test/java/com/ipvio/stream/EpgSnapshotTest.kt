package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/** Stored read guide (T-059). */
class EpgSnapshotTest {
    private val hour = 3_600_000L
    private val data = EpgData(
        mapOf("162" to listOf(EpgEntry("Новости", "desc", 100L, 200L), EpgEntry("Фильм", "", 200L, 300L))),
        mapOf("НТВ HD" to "162"), mapOf("162" to "http://epg.one/img2/162.png"), mapOf("Канал" to "http://epg.one/img2/1.png")
    )

    @Test fun roundTripKeepsShowsMatchesAndIcons() {
        val b = ByteArrayOutputStream()
        EpgSnapshot.write(b, data, 1234L, 99L)
        val r = EpgSnapshot.read(ByteArrayInputStream(b.toByteArray()))!!
        assertEquals(1234L, r.savedAt)
        assertEquals(99L, r.sig)
        assertEquals(data.progs, r.data.progs)
        assertEquals(data.byTitle, r.data.byTitle)
        assertEquals(data.icons, r.data.icons)
        assertEquals(data.iconByTitle, r.data.iconByTitle)
    }

    @Test fun damagedOrForeignFileIsIgnored() {
        assertNull(EpgSnapshot.read(ByteArrayInputStream(byteArrayOf(1, 2, 3))))
        val b = ByteArrayOutputStream()
        EpgSnapshot.write(b, data, 1L, 2L)
        val cut = b.toByteArray().copyOf(b.size() / 2)
        assertNull(EpgSnapshot.read(ByteArrayInputStream(cut)))
    }

    @Test fun copyIsUsedFor36HoursForTheSameChannels() {
        assertTrue(EpgSnapshot.usable(0L, 5L, 5L, 35 * hour))
        assertFalse(EpgSnapshot.usable(0L, 5L, 5L, 36 * hour))
        assertFalse(EpgSnapshot.usable(0L, 5L, 6L, hour))
        assertFalse(EpgSnapshot.usable(10 * hour, 5L, 5L, hour))
    }

    @Test fun refreshEvery12Hours() {
        assertTrue(EpgSnapshot.refreshDue(0L, 5 * hour))
        assertFalse(EpgSnapshot.refreshDue(hour, 12 * hour))
        assertTrue(EpgSnapshot.refreshDue(hour, 13 * hour))
        assertTrue(EpgSnapshot.refreshDue(10 * hour, hour))
    }

    @Test fun failedReadDoesNotWipeTheShownGuide() {
        assertFalse(EpgSnapshot.replace(data, EpgData.EMPTY, sameChannels = true))
        assertTrue(EpgSnapshot.replace(data, EpgData.EMPTY, sameChannels = false))
        assertTrue(EpgSnapshot.replace(EpgData.EMPTY, EpgData.EMPTY, sameChannels = true))
        assertTrue(EpgSnapshot.replace(EpgData.EMPTY, data, sameChannels = true))
    }

    @Test fun signatureDependsOnContentNotOrder() {
        val a = EpgSnapshot.sig(listOf("http://a", "http://b"), setOf("x", "y"), setOf("нтв"), mapOf("x" to 3))
        val b = EpgSnapshot.sig(listOf(" http://b", "http://a"), setOf("y", "x"), setOf("нтв"), mapOf("x" to 3))
        assertEquals(a, b)
        assertFalse(a == EpgSnapshot.sig(listOf("http://a"), setOf("x", "y"), setOf("нтв"), mapOf("x" to 3)))
        assertFalse(a == EpgSnapshot.sig(listOf("http://a", "http://b"), setOf("x", "y"), setOf("нтв"), mapOf("x" to 4)))
    }
}

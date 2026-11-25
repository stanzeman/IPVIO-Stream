package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Remembered sort of category views (T-062). */
class BrowseSortLogicTest {
    private val n = 9

    @Test fun categoryOpensWithTheLastPickedSort() = assertEquals(1, BrowseSortLogic.initial(1, -1, 0, n))

    @Test fun rememberedSortWinsOverTheFixedDefault() = assertEquals(1, BrowseSortLogic.initial(1, 4, 0, n))

    @Test fun withoutRememberedSortTheFixedDefaultOrTheCategoryDefaultApplies() {
        assertEquals(4, BrowseSortLogic.initial(-1, 4, 0, n))
        assertEquals(0, BrowseSortLogic.initial(-1, -1, 0, n))
    }

    @Test fun listsWithOwnOrderKeepIt() {
        assertEquals(2, BrowseSortLogic.initial(1, -1, 2, n))
        assertEquals(8, BrowseSortLogic.initial(1, -1, 8, n))
        assertFalse(BrowseSortLogic.remembers(2))
        assertTrue(BrowseSortLogic.remembers(0))
    }

    @Test fun storedValueOutOfRangeIsIgnored() = assertEquals(0, BrowseSortLogic.initial(42, -1, 0, n))
}

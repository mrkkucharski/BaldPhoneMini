package com.bald.uriah.baldphone.adapters

import org.junit.Assert.assertEquals
import org.junit.Test

class BaldPagerAdapterTest {
    @Test
    fun calculatePinnedPageCountKeepsEmptyPinsOffExtraPages() {
        assertEquals(0, BaldPagerAdapter.calculatePinnedPageCount(0))
    }

    @Test
    fun calculatePinnedPageCountUsesOnePageForUpToEightPins() {
        assertEquals(1, BaldPagerAdapter.calculatePinnedPageCount(1))
        assertEquals(1, BaldPagerAdapter.calculatePinnedPageCount(8))
    }

    @Test
    fun calculatePinnedPageCountAddsPagesForOverflowPins() {
        assertEquals(2, BaldPagerAdapter.calculatePinnedPageCount(9))
        assertEquals(2, BaldPagerAdapter.calculatePinnedPageCount(16))
        assertEquals(3, BaldPagerAdapter.calculatePinnedPageCount(17))
    }

    @Test
    fun calculateItemCountAddsPinnedPagesAfterFixedHomePages() {
        assertEquals(3, BaldPagerAdapter.calculateItemCount(3, 0))
        assertEquals(4, BaldPagerAdapter.calculateItemCount(3, 8))
        assertEquals(5, BaldPagerAdapter.calculateItemCount(3, 9))
    }
}

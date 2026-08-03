package com.starlive.ring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CropStrategyTest {

    @Test
    fun exact_2990x284() {
        assertEquals(CropStrategy.Strategy.EXACT, CropStrategy.choose(2990, 284))
        assertEquals(CropStrategy.Strategy.EXACT, CropStrategy.choose(2991, 285))
        assertEquals(CropStrategy.Strategy.EXACT, CropStrategy.choose(2988, 282))
    }

    @Test
    fun band_4032x284() {
        assertEquals(CropStrategy.Strategy.BAND, CropStrategy.choose(4032, 284))
        assertEquals(CropStrategy.Strategy.BAND, CropStrategy.choose(4200, 300))
    }

    @Test
    fun center_arbitrary() {
        assertEquals(CropStrategy.Strategy.CENTER, CropStrategy.choose(1920, 1080))
        assertEquals(CropStrategy.Strategy.CENTER, CropStrategy.choose(800, 600))
        assertEquals(CropStrategy.Strategy.CENTER, CropStrategy.choose(100, 100))
    }

    @Test
    fun valid_bounds() {
        assertTrue(CropStrategy.isValidBounds(8, 8))
        assertTrue(CropStrategy.isValidBounds(2990, 284))
        assertFalse(CropStrategy.isValidBounds(7, 8))
        assertFalse(CropStrategy.isValidBounds(8, 0))
        assertFalse(CropStrategy.isValidBounds(-1, 284))
    }

    @Test
    fun labels_zh_non_empty() {
        for (s in CropStrategy.Strategy.entries) {
            assertTrue(CropStrategy.labelZh(s).isNotBlank())
        }
    }
}

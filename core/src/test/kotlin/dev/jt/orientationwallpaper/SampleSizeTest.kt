package dev.jt.orientationwallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleSizeTest {
    @Test
    fun staysAtOneWhenTheSourceAlreadyFits() {
        assertEquals(1, SampleSize.forOriented(100, 100, 100, 100, swapsAxes = false))
        assertEquals(1, SampleSize.forOriented(80, 80, 100, 100, swapsAxes = false))
        assertEquals(1, SampleSize.forOriented(0, 100, 10, 10, swapsAxes = false))
        assertEquals(1, SampleSize.forOriented(100, 100, 0, 10, swapsAxes = false))
    }

    @Test
    fun usesTheLargestPowerOfTwoThatStillCovers() {
        assertEquals(4, SampleSize.forOriented(400, 400, 100, 100, swapsAxes = false))
        assertEquals(2, SampleSize.forOriented(300, 300, 100, 100, swapsAxes = false))
        assertEquals(1, SampleSize.forOriented(150, 150, 100, 100, swapsAxes = false))
    }

    @Test
    fun swapsAxesBeforeSamplingWhenExifRotates() {
        assertEquals(
            1,
            SampleSize.forOriented(4000, 2000, 1000, 1500, swapsAxes = false),
        )
        assertEquals(
            2,
            SampleSize.forOriented(4000, 2000, 1000, 1500, swapsAxes = true),
        )
    }

    @Test
    fun doesNotUndershootAPortraitScreenAfterRotation() {
        // 8000x6000 stored landscape, displayed portrait (EXIF 90). Phone is 1080x2400.
        assertEquals(
            2,
            SampleSize.forOriented(8000, 6000, 1080, 2400, swapsAxes = true),
        )
    }
}

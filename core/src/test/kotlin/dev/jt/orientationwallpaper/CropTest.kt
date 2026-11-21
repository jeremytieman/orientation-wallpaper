package dev.jt.orientationwallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropTest {
    @Test
    fun wideSourceCropsTheSides() {
        val crop = CropMath.centerCrop(200, 100, 100, 100)
        assertEquals(1f, crop.scale, 0.001f)
        assertEquals(-50f, crop.dx, 0.001f)
        assertEquals(0f, crop.dy, 0.001f)
        assertCovers(srcWidth = 200, srcHeight = 100, dstWidth = 100, dstHeight = 100, crop = crop)
    }

    @Test
    fun tallSourceCropsTheTopAndBottom() {
        val crop = CropMath.centerCrop(100, 200, 100, 100)
        assertEquals(1f, crop.scale, 0.001f)
        assertEquals(0f, crop.dx, 0.001f)
        assertEquals(-50f, crop.dy, 0.001f)
    }

    @Test
    fun scalesUpToCoverAWiderDestination() {
        val crop = CropMath.centerCrop(100, 100, 200, 100)
        assertEquals(2f, crop.scale, 0.001f)
        assertEquals(0f, crop.dx, 0.001f)
        assertEquals(-50f, crop.dy, 0.001f)
        assertCovers(100, 100, 200, 100, crop)
    }

    @Test
    fun sameAspectHasNoOverflow() {
        val crop = CropMath.centerCrop(1080, 1920, 1080, 1920)
        assertEquals(1f, crop.scale, 0.001f)
        assertEquals(0f, crop.dx, 0.001f)
        assertEquals(0f, crop.dy, 0.001f)
    }

    @Test
    fun emptyDimensionsStayPut() {
        assertEquals(CenterCrop(1f, 0f, 0f), CropMath.centerCrop(0, 10, 10, 10))
        assertEquals(CenterCrop(1f, 0f, 0f), CropMath.centerCrop(10, 10, 0, 10))
    }

    private fun assertCovers(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int, crop: CenterCrop) {
        val drawnWidth = srcWidth * crop.scale
        val drawnHeight = srcHeight * crop.scale
        assertTrue(drawnWidth + 0.01f >= dstWidth)
        assertTrue(drawnHeight + 0.01f >= dstHeight)
        assertEquals((dstWidth - drawnWidth) / 2f, crop.dx, 0.01f)
        assertEquals((dstHeight - drawnHeight) / 2f, crop.dy, 0.01f)
    }
}

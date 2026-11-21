package dev.jt.orientationwallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifOrientationsTest {
    @Test
    fun mapsEveryOrientationTag() {
        assertEquals(ExifTransform(0, flipHorizontalAfterRotation = false), ExifOrientations.transformFor(ExifOrientations.NORMAL))
        assertEquals(ExifTransform(0, flipHorizontalAfterRotation = true), ExifOrientations.transformFor(ExifOrientations.FLIP_HORIZONTAL))
        assertEquals(ExifTransform(180, flipHorizontalAfterRotation = false), ExifOrientations.transformFor(ExifOrientations.ROTATE_180))
        assertEquals(ExifTransform(180, flipHorizontalAfterRotation = true), ExifOrientations.transformFor(ExifOrientations.FLIP_VERTICAL))
        assertEquals(ExifTransform(90, flipHorizontalAfterRotation = true), ExifOrientations.transformFor(ExifOrientations.TRANSPOSE))
        assertEquals(ExifTransform(90, flipHorizontalAfterRotation = false), ExifOrientations.transformFor(ExifOrientations.ROTATE_90))
        assertEquals(ExifTransform(270, flipHorizontalAfterRotation = true), ExifOrientations.transformFor(ExifOrientations.TRANSVERSE))
        assertEquals(ExifTransform(270, flipHorizontalAfterRotation = false), ExifOrientations.transformFor(ExifOrientations.ROTATE_270))
    }

    @Test
    fun unknownOrientationIsIdentity() {
        val transform = ExifOrientations.transformFor(0)
        assertTrue(transform.isIdentity)
        assertFalse(transform.swapsAxes)
    }

    @Test
    fun quarterTurnsSwapAxes() {
        assertFalse(ExifOrientations.swapsAxes(ExifOrientations.NORMAL))
        assertFalse(ExifOrientations.swapsAxes(ExifOrientations.ROTATE_180))
        assertTrue(ExifOrientations.swapsAxes(ExifOrientations.ROTATE_90))
        assertTrue(ExifOrientations.swapsAxes(ExifOrientations.ROTATE_270))
        assertTrue(ExifOrientations.swapsAxes(ExifOrientations.TRANSPOSE))
        assertTrue(ExifOrientations.swapsAxes(ExifOrientations.TRANSVERSE))
    }
}

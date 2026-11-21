package dev.jt.orientationwallpaper

import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationTest {
    @Test
    fun choosesLandscapeWhenTheSurfaceIsWiderOrSquare() {
        assertEquals(WallpaperSlot.LANDSCAPE, slotForSurface(1920, 1080))
        assertEquals(WallpaperSlot.LANDSCAPE, slotForSurface(2560, 1600))
        assertEquals(WallpaperSlot.LANDSCAPE, slotForSurface(1000, 1000))
    }

    @Test
    fun choosesPortraitWhenTheSurfaceIsTaller() {
        assertEquals(WallpaperSlot.PORTRAIT, slotForSurface(1080, 2400))
        assertEquals(WallpaperSlot.PORTRAIT, slotForSurface(1600, 2560))
    }

    @Test
    fun decodesEachOrientationTowardTheFullDisplay() {
        val portrait = DecodeTargets.forSlot(
            slot = WallpaperSlot.PORTRAIT,
            displayWidth = 1080,
            displayHeight = 2400,
            surfaceWidth = 1080,
            surfaceHeight = 2400,
            preview = false,
        )
        val landscape = DecodeTargets.forSlot(
            slot = WallpaperSlot.LANDSCAPE,
            displayWidth = 1080,
            displayHeight = 2400,
            surfaceWidth = 1080,
            surfaceHeight = 2400,
            preview = false,
        )
        assertEquals(PixelSize(1080, 2400), portrait)
        assertEquals(PixelSize(2400, 1080), landscape)
    }

    @Test
    fun usesTheLargerOfDisplayAndMatchingSurface() {
        val portrait = DecodeTargets.forSlot(
            slot = WallpaperSlot.PORTRAIT,
            displayWidth = 1000,
            displayHeight = 2000,
            surfaceWidth = 1200,
            surfaceHeight = 2600,
            preview = false,
        )
        val landscape = DecodeTargets.forSlot(
            slot = WallpaperSlot.LANDSCAPE,
            displayWidth = 1000,
            displayHeight = 2000,
            surfaceWidth = 1200,
            surfaceHeight = 2600,
            preview = false,
        )
        assertEquals(PixelSize(1200, 2600), portrait)
        assertEquals(PixelSize(2000, 1000), landscape)
    }

    @Test
    fun tabletLandscapeSurfaceStillPreparesAPortraitBitmap() {
        val portrait = DecodeTargets.forSlot(
            slot = WallpaperSlot.PORTRAIT,
            displayWidth = 2560,
            displayHeight = 1600,
            surfaceWidth = 2560,
            surfaceHeight = 1600,
            preview = false,
        )
        val landscape = DecodeTargets.forSlot(
            slot = WallpaperSlot.LANDSCAPE,
            displayWidth = 2560,
            displayHeight = 1600,
            surfaceWidth = 2560,
            surfaceHeight = 1600,
            preview = false,
        )
        assertEquals(PixelSize(1600, 2560), portrait)
        assertEquals(PixelSize(2560, 1600), landscape)
    }

    @Test
    fun previewDecodesTowardThePreviewSurface() {
        val portrait = DecodeTargets.forSlot(
            slot = WallpaperSlot.PORTRAIT,
            displayWidth = 2560,
            displayHeight = 1600,
            surfaceWidth = 400,
            surfaceHeight = 800,
            preview = true,
        )
        val landscape = DecodeTargets.forSlot(
            slot = WallpaperSlot.LANDSCAPE,
            displayWidth = 2560,
            displayHeight = 1600,
            surfaceWidth = 400,
            surfaceHeight = 800,
            preview = true,
        )
        assertEquals(PixelSize(400, 800), portrait)
        assertEquals(PixelSize(800, 400), landscape)
    }

    @Test
    fun capsTheLongEdge() {
        val landscape = DecodeTargets.forSlot(
            slot = WallpaperSlot.LANDSCAPE,
            displayWidth = 8192,
            displayHeight = 2048,
            surfaceWidth = 8192,
            surfaceHeight = 2048,
            preview = false,
        )
        assertEquals(PixelSize(4096, 1024), landscape)
    }
}

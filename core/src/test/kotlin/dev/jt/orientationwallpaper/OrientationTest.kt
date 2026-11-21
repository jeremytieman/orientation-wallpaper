package dev.jt.orientationwallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrientationTest {
    @Test
    fun choosesLandscapeWhenTheDisplayIsWiderOrSquare() {
        assertEquals(WallpaperSlot.LANDSCAPE, wallpaperSlot(1920, 1080, 1920, 1080))
        assertEquals(WallpaperSlot.LANDSCAPE, wallpaperSlot(2560, 1600, 2560, 1600))
        assertEquals(WallpaperSlot.LANDSCAPE, wallpaperSlot(1000, 1000, 1000, 1000))
    }

    @Test
    fun choosesPortraitWhenTheDisplayIsTaller() {
        assertEquals(WallpaperSlot.PORTRAIT, wallpaperSlot(1080, 2400, 1080, 2400))
        assertEquals(WallpaperSlot.PORTRAIT, wallpaperSlot(1600, 2560, 1600, 2560))
    }

    @Test
    fun wideScrollingSurfaceDoesNotOverrideAPortraitDisplay() {
        assertEquals(WallpaperSlot.PORTRAIT, wallpaperSlot(1080, 2400, 3240, 2400))
    }

    @Test
    fun tallSurfaceDoesNotOverrideALandscapeDisplay() {
        assertEquals(WallpaperSlot.LANDSCAPE, wallpaperSlot(2400, 1080, 1080, 3200))
    }

    @Test
    fun fallsBackToTheSurfaceWhenTheDisplaySizeIsUnknown() {
        assertEquals(WallpaperSlot.PORTRAIT, wallpaperSlot(0, 0, 1080, 2400))
        assertEquals(WallpaperSlot.LANDSCAPE, wallpaperSlot(0, 0, 1920, 1080))
    }

    @Test
    fun fixedSurfaceSwitchesImageWhenTheDisplayRotates() {
        val surfaceWidth = 3000
        val surfaceHeight = 2400
        assertEquals(WallpaperSlot.PORTRAIT, wallpaperSlot(1080, 2400, surfaceWidth, surfaceHeight))
        assertEquals(WallpaperSlot.LANDSCAPE, wallpaperSlot(2400, 1080, surfaceWidth, surfaceHeight))
        assertTrue(displaySizeChanged(1080, 2400, 2400, 1080))
        assertFalse(displaySizeChanged(1080, 2400, 1080, 2400))
    }

    @Test
    fun listensForDisplayChangesOnlyWhileVisible() {
        assertTrue(shouldListenForDisplayChanges(visible = true, destroyed = false))
        assertFalse(shouldListenForDisplayChanges(visible = false, destroyed = false))
        assertFalse(shouldListenForDisplayChanges(visible = true, destroyed = true))
        assertFalse(shouldListenForDisplayChanges(visible = false, destroyed = true))
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

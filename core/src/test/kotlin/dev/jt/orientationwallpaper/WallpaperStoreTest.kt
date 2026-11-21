package dev.jt.orientationwallpaper

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperStoreTest {
    @Test
    fun copiesASupportedImageIntoTheSlot() {
        val store = newStore()
        val format = store.import(WallpaperSlot.PORTRAIT, png().inputStream())
        assertEquals(ImageFormat.PNG, format)
        val file = store.imageFile(WallpaperSlot.PORTRAIT)
        assertEquals("portrait.png", file?.name)
        assertTrue(file!!.length() > 0)
        assertEquals(1L, store.revision())
        assertNull(store.imageFile(WallpaperSlot.LANDSCAPE))
    }

    @Test
    fun replacingASlotRemovesThePreviousExtension() {
        val store = newStore()
        store.import(WallpaperSlot.PORTRAIT, png().inputStream())
        store.import(WallpaperSlot.PORTRAIT, jpeg().inputStream())
        assertEquals("portrait.jpg", store.imageFile(WallpaperSlot.PORTRAIT)?.name)
        assertEquals(2L, store.revision())
        val leftovers = store.directory.listFiles()?.map { it.name }?.filter { it.startsWith("portrait.") }
        assertEquals(listOf("portrait.jpg"), leftovers)
    }

    @Test
    fun onePickCanFillBothOrientations() {
        val store = newStore()
        store.importBoth(webp().inputStream())
        val portrait = store.imageFile(WallpaperSlot.PORTRAIT)!!
        val landscape = store.imageFile(WallpaperSlot.LANDSCAPE)!!
        assertEquals("portrait.webp", portrait.name)
        assertEquals("landscape.webp", landscape.name)
        assertEquals(portrait.readBytes().toList(), landscape.readBytes().toList())
        assertEquals(1L, store.revision())
    }

    @Test
    fun rejectsUnsupportedBytes() {
        val store = newStore()
        try {
            store.import(WallpaperSlot.LANDSCAPE, "hello world!!".encodeToByteArray().inputStream())
            throw AssertionError("expected UnsupportedImageFormatException")
        } catch (expected: UnsupportedImageFormatException) {
            // expected
        }
        assertNull(store.imageFile(WallpaperSlot.LANDSCAPE))
        assertEquals(0L, store.revision())
    }

    @Test
    fun ignoresUnsafeNamesInTheStateFile() {
        val store = newStore()
        File(store.directory, "state.properties").writeText(
            "revision=4\nportrait=../secret.png\nlandscape=portrait.png\n",
        )
        File(store.directory, "portrait.png").writeBytes(png())
        assertNull(store.imageFile(WallpaperSlot.PORTRAIT))
        assertEquals("portrait.png", store.imageFile(WallpaperSlot.LANDSCAPE)?.name)
    }

    @Test
    fun stagingLeavesTheCurrentImageUntilCommit() {
        val store = newStore()
        store.import(WallpaperSlot.PORTRAIT, png().inputStream())
        val staged = store.stage(jpeg().inputStream())
        assertEquals("portrait.png", store.imageFile(WallpaperSlot.PORTRAIT)?.name)
        assertEquals(1L, store.revision())
        store.discard(staged)
        assertFalse(staged.file.exists())
        assertEquals("portrait.png", store.imageFile(WallpaperSlot.PORTRAIT)?.name)
        assertEquals(1L, store.revision())
    }

    @Test
    fun commitReplacesTheSlotAtomicallyAfterStaging() {
        val store = newStore()
        store.import(WallpaperSlot.PORTRAIT, png().inputStream())
        val staged = store.stage(jpeg().inputStream())
        store.commit(WallpaperSlot.PORTRAIT, staged)
        store.discard(staged)
        assertEquals("portrait.jpg", store.imageFile(WallpaperSlot.PORTRAIT)?.name)
        assertEquals(2L, store.revision())
        val leftovers = store.directory.listFiles()?.map { it.name }?.filter { it.startsWith("portrait.") }
        assertEquals(listOf("portrait.jpg"), leftovers)
    }

    @Test
    fun clearDropsOneSlot() {
        val store = newStore()
        store.importBoth(png().inputStream())
        store.clear(WallpaperSlot.PORTRAIT)
        assertNull(store.imageFile(WallpaperSlot.PORTRAIT))
        assertEquals("landscape.png", store.imageFile(WallpaperSlot.LANDSCAPE)?.name)
        assertEquals(2L, store.revision())
    }

    private fun newStore(): WallpaperStore {
        val dir = Files.createTempDirectory("wallpapers").toFile()
        return WallpaperStore(dir)
    }

    private fun png(): ByteArray {
        return byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()) +
            ByteArray(24)
    }

    private fun jpeg(): ByteArray {
        return byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(24)
    }

    private fun webp(): ByteArray {
        return "RIFF".encodeToByteArray() + byteArrayOf(24, 0, 0, 0) + "WEBP".encodeToByteArray() + ByteArray(16)
    }
}

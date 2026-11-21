package dev.jt.orientationwallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageFormatsTest {
    @Test
    fun identifiesSupportedHeaders() {
        assertEquals(ImageFormat.PNG, ImageFormats.identify(png()))
        assertEquals(ImageFormat.JPEG, ImageFormats.identify(jpeg()))
        assertEquals(ImageFormat.WEBP, ImageFormats.identify(webp()))
        assertEquals(ImageFormat.AVIF, ImageFormats.identify(ftyp("avif")))
        assertEquals(ImageFormat.AVIF, ImageFormats.identify(ftyp("avis")))
        assertEquals(ImageFormat.HEIC, ImageFormats.identify(ftyp("heic")))
        assertEquals(ImageFormat.HEIF, ImageFormats.identify(ftyp("mif1")))
    }

    @Test
    fun rejectsOtherFiles() {
        assertNull(ImageFormats.identify(byteArrayOf()))
        assertNull(ImageFormats.identify("GIF89a.......".encodeToByteArray()))
        assertNull(ImageFormats.identify("not an image!!".encodeToByteArray()))
        assertNull(ImageFormats.identify(ftyp("mp42")))
    }

    @Test
    fun readsTheFormatFromAFileName() {
        assertEquals(ImageFormat.JPEG, ImageFormats.fromFileName("portrait.jpg"))
        assertEquals(ImageFormat.HEIC, ImageFormats.fromFileName("landscape.heic"))
        assertNull(ImageFormats.fromFileName("notes.txt"))
    }

    private fun png(): ByteArray {
        return byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte()) +
            ByteArray(16)
    }

    private fun jpeg(): ByteArray = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(16)

    private fun webp(): ByteArray {
        return "RIFF".encodeToByteArray() + byteArrayOf(0, 0, 0, 0) + "WEBP".encodeToByteArray()
    }

    private fun ftyp(brand: String): ByteArray {
        return byteArrayOf(0, 0, 0, 0x18) + "ftyp".encodeToByteArray() + brand.encodeToByteArray() + ByteArray(8)
    }
}

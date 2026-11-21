package dev.jt.orientationwallpaper

enum class ImageFormat(val extension: String, val label: String) {
    PNG("png", "PNG"),
    JPEG("jpg", "JPEG"),
    WEBP("webp", "WebP"),
    AVIF("avif", "AVIF"),
    HEIC("heic", "HEIC"),
    HEIF("heif", "HEIF"),
}

/**
 * Sniffs a file header. The Storage Access Framework mime type is only a hint;
 * the bytes decide whether the file is one of the formats this app stores.
 * Decoding still depends on the platform (AVIF in particular).
 */
object ImageFormats {
    val pickerMimeTypes: Array<String> = arrayOf(
        "image/*",
        "image/heic",
        "image/heif",
        "image/avif",
    )

    fun identify(header: ByteArray): ImageFormat? {
        if (header.size < 12) return null
        if (isPng(header)) return ImageFormat.PNG
        if (isJpeg(header)) return ImageFormat.JPEG
        if (isWebp(header)) return ImageFormat.WEBP
        if (header.copyOfRange(4, 8).contentEquals(FTYP)) {
            val brand = header.copyOfRange(8, 12).toString(Charsets.US_ASCII)
            return when (brand) {
                "avif", "avis" -> ImageFormat.AVIF
                "heic", "heix", "hevc", "hevx" -> ImageFormat.HEIC
                "mif1", "msf1", "heim", "heis", "hevm", "hevs" -> ImageFormat.HEIF
                else -> null
            }
        }
        return null
    }

    fun fromFileName(name: String): ImageFormat? {
        val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return ImageFormat.entries.firstOrNull { it.extension == extension }
    }

    private fun isPng(header: ByteArray): Boolean {
        return header[0] == 0x89.toByte() &&
            header[1] == 'P'.code.toByte() &&
            header[2] == 'N'.code.toByte() &&
            header[3] == 'G'.code.toByte()
    }

    private fun isJpeg(header: ByteArray): Boolean {
        return header[0] == 0xFF.toByte() &&
            header[1] == 0xD8.toByte() &&
            header[2] == 0xFF.toByte()
    }

    private fun isWebp(header: ByteArray): Boolean {
        return header.copyOfRange(0, 4).contentEquals(RIFF) &&
            header.copyOfRange(8, 12).contentEquals(WEBP)
    }

    private val RIFF = byteArrayOf(0x52, 0x49, 0x46, 0x46)
    private val WEBP = byteArrayOf(0x57, 0x45, 0x42, 0x50)
    private val FTYP = byteArrayOf(0x66, 0x74, 0x79, 0x70)
}

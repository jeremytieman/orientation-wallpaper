package dev.jt.orientationwallpaper

/**
 * Which stored image the wallpaper draws.
 *
 * A square display counts as landscape (`width >= height`).
 */
enum class WallpaperSlot(val filePrefix: String) {
    PORTRAIT("portrait"),
    LANDSCAPE("landscape"),
}

/**
 * Image to draw for this display.
 *
 * The wallpaper surface is not the display. Launchers call
 * `suggestDesiredDimensions` with a surface wider than the screen so a wallpaper
 * can scroll, and that surface can be landscape-shaped while the device is
 * portrait. The display size decides. The surface size is only a fallback for
 * the moment before the display size is known.
 */
fun wallpaperSlot(
    displayWidth: Int,
    displayHeight: Int,
    surfaceWidth: Int,
    surfaceHeight: Int,
): WallpaperSlot {
    val width: Int
    val height: Int
    if (displayWidth > 0 && displayHeight > 0) {
        width = displayWidth
        height = displayHeight
    } else {
        width = surfaceWidth
        height = surfaceHeight
    }
    return if (width >= height) WallpaperSlot.LANDSCAPE else WallpaperSlot.PORTRAIT
}

/** Aspect of one rectangle. Used for decode sizing, not for choosing the image. */
fun slotForSize(width: Int, height: Int): WallpaperSlot {
    return if (width >= height) WallpaperSlot.LANDSCAPE else WallpaperSlot.PORTRAIT
}

data class PixelSize(val width: Int, val height: Int) {
    init {
        require(width >= 0 && height >= 0) { "size must be non-negative, was ${width}x$height" }
    }
}

/**
 * How large to decode an orientation's bitmap.
 *
 * The live wallpaper decodes toward the full display so a later rotation still
 * has enough pixels. A system preview decodes toward the preview surface instead.
 * The long edge is capped so a huge external display cannot force an OOM.
 */
object DecodeTargets {
    const val MAX_EDGE = 4096

    fun forSlot(
        slot: WallpaperSlot,
        displayWidth: Int,
        displayHeight: Int,
        surfaceWidth: Int,
        surfaceHeight: Int,
        preview: Boolean,
    ): PixelSize {
        val base = if (preview && surfaceWidth > 0 && surfaceHeight > 0) {
            oriented(slot, surfaceWidth, surfaceHeight)
        } else {
            val full = oriented(slot, displayWidth, displayHeight)
            if (
                surfaceWidth > 0 &&
                surfaceHeight > 0 &&
                slotForSize(surfaceWidth, surfaceHeight) == slot
            ) {
                PixelSize(
                    width = maxOf(full.width, surfaceWidth),
                    height = maxOf(full.height, surfaceHeight),
                )
            } else {
                full
            }
        }
        return cap(base)
    }

    private fun oriented(slot: WallpaperSlot, width: Int, height: Int): PixelSize {
        val longSide = maxOf(width, height).coerceAtLeast(1)
        val shortSide = minOf(width, height).coerceAtLeast(1)
        return if (slot == WallpaperSlot.PORTRAIT) {
            PixelSize(shortSide, longSide)
        } else {
            PixelSize(longSide, shortSide)
        }
    }

    private fun cap(size: PixelSize): PixelSize {
        val longSide = maxOf(size.width, size.height)
        if (longSide <= MAX_EDGE) return size
        val scale = MAX_EDGE.toFloat() / longSide.toFloat()
        return PixelSize(
            width = (size.width * scale).toInt().coerceAtLeast(1),
            height = (size.height * scale).toInt().coerceAtLeast(1),
        )
    }
}

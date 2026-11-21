package dev.jt.orientationwallpaper

/**
 * Center-crop placement of a source bitmap onto a destination surface.
 *
 * [scale] is applied around the origin, then [dx] and [dy] translate in
 * destination pixels. The scaled bitmap covers the destination on both axes
 * and the overflow is split evenly.
 */
data class CenterCrop(val scale: Float, val dx: Float, val dy: Float)

object CropMath {
    fun centerCrop(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): CenterCrop {
        if (srcWidth <= 0 || srcHeight <= 0 || dstWidth <= 0 || dstHeight <= 0) {
            return CenterCrop(scale = 1f, dx = 0f, dy = 0f)
        }
        val scale = maxOf(
            dstWidth / srcWidth.toFloat(),
            dstHeight / srcHeight.toFloat(),
        )
        val dx = (dstWidth - srcWidth * scale) / 2f
        val dy = (dstHeight - srcHeight * scale) / 2f
        return CenterCrop(scale, dx, dy)
    }
}

/**
 * Power-of-two sample size that keeps both oriented dimensions at least the
 * requested size when the source is larger. This is the standard BitmapFactory
 * `inSampleSize` loop, with an optional axis swap for EXIF rotations of 90 or 270.
 */
object SampleSize {
    fun forOriented(
        srcWidth: Int,
        srcHeight: Int,
        reqWidth: Int,
        reqHeight: Int,
        swapsAxes: Boolean,
    ): Int {
        if (srcWidth <= 0 || srcHeight <= 0 || reqWidth <= 0 || reqHeight <= 0) return 1
        val width = if (swapsAxes) srcHeight else srcWidth
        val height = if (swapsAxes) srcWidth else srcHeight
        var sample = 1
        if (width > reqWidth || height > reqHeight) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / sample >= reqWidth && halfHeight / sample >= reqHeight) {
                sample *= 2
            }
        }
        return sample
    }
}

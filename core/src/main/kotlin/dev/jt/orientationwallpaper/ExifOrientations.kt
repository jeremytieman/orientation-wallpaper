package dev.jt.orientationwallpaper

/**
 * EXIF orientation tag values. They match `androidx.exifinterface.media.ExifInterface`
 * and the platform constants, so the decoder can pass the tag through unchanged.
 *
 * The transform is "rotate clockwise, then flip horizontally", which is the
 * matrix sequence BitmapFactory needs because it does not apply EXIF itself.
 * [android.graphics.ImageDecoder] applies EXIF on its own and must not use this.
 */
data class ExifTransform(
    val rotationDegreesClockwise: Int,
    val flipHorizontalAfterRotation: Boolean,
) {
    val isIdentity: Boolean
        get() = rotationDegreesClockwise == 0 && !flipHorizontalAfterRotation

    val swapsAxes: Boolean
        get() = rotationDegreesClockwise == 90 || rotationDegreesClockwise == 270
}

object ExifOrientations {
    const val NORMAL = 1
    const val FLIP_HORIZONTAL = 2
    const val ROTATE_180 = 3
    const val FLIP_VERTICAL = 4
    const val TRANSPOSE = 5
    const val ROTATE_90 = 6
    const val TRANSVERSE = 7
    const val ROTATE_270 = 8

    fun transformFor(orientation: Int): ExifTransform = when (orientation) {
        FLIP_HORIZONTAL -> ExifTransform(0, flipHorizontalAfterRotation = true)
        ROTATE_180 -> ExifTransform(180, flipHorizontalAfterRotation = false)
        FLIP_VERTICAL -> ExifTransform(180, flipHorizontalAfterRotation = true)
        TRANSPOSE -> ExifTransform(90, flipHorizontalAfterRotation = true)
        ROTATE_90 -> ExifTransform(90, flipHorizontalAfterRotation = false)
        TRANSVERSE -> ExifTransform(270, flipHorizontalAfterRotation = true)
        ROTATE_270 -> ExifTransform(270, flipHorizontalAfterRotation = false)
        else -> ExifTransform(0, flipHorizontalAfterRotation = false)
    }

    fun swapsAxes(orientation: Int): Boolean = transformFor(orientation).swapsAxes
}

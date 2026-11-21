package dev.jt.orientationwallpaper.wallpaper

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.os.Build
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import dev.jt.orientationwallpaper.ExifOrientations
import dev.jt.orientationwallpaper.PixelSize
import dev.jt.orientationwallpaper.SampleSize
import java.io.File
import java.io.IOException

/**
 * Decodes a stored wallpaper down to about the screen size.
 *
 * API 28 and newer use [ImageDecoder], which applies EXIF orientation itself.
 * Older releases (and any file ImageDecoder refuses) go through [BitmapFactory]
 * plus [ExifInterface]. Either path subsamples so a camera original is not
 * decoded at full resolution.
 */
class WallpaperBitmapDecoder {
    fun decode(file: File, target: PixelSize): Bitmap {
        try {
            return decodeInternal(file, target)
        } catch (oom: OutOfMemoryError) {
            throw IOException("Not enough memory to decode ${file.name}", oom)
        }
    }

    private fun decodeInternal(file: File, target: PixelSize): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return decodeResilient { extraSample ->
                    decodeWithImageDecoder(file, target, extraSample)
                }
            } catch (error: IOException) {
                Log.w(TAG, "ImageDecoder failed for ${file.name}; trying BitmapFactory", error)
            }
        }
        return decodeResilient { extraSample ->
            decodeWithBitmapFactory(file, target, extraSample)
        }
    }

    private fun decodeResilient(block: (extraSample: Int) -> Bitmap): Bitmap {
        var extraSample = 1
        var last: OutOfMemoryError? = null
        repeat(6) {
            try {
                return block(extraSample)
            } catch (oom: OutOfMemoryError) {
                last = oom
                extraSample *= 2
            }
        }
        throw last ?: IOException("Could not decode image")
    }

    @SuppressLint("NewApi")
    private fun decodeWithImageDecoder(file: File, target: PixelSize, extraSample: Int): Bitmap {
        val source = ImageDecoder.createSource(file)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val sample = SampleSize.forOriented(
                srcWidth = info.size.width,
                srcHeight = info.size.height,
                reqWidth = target.width,
                reqHeight = target.height,
                swapsAxes = false,
            ) * extraSample
            decoder.setTargetSize(
                (info.size.width / sample).coerceAtLeast(1),
                (info.size.height / sample).coerceAtLeast(1),
            )
        }
    }

    private fun decodeWithBitmapFactory(file: File, target: PixelSize, extraSample: Int): Bitmap {
        val orientation = readExifOrientation(file)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Could not read ${file.name}")
        }
        val sample = SampleSize.forOriented(
            srcWidth = bounds.outWidth,
            srcHeight = bounds.outHeight,
            reqWidth = target.width,
            reqHeight = target.height,
            swapsAxes = ExifOrientations.swapsAxes(orientation),
        ) * extraSample
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw IOException("Could not decode ${file.name}")
        return applyExif(decoded, orientation)
    }

    private fun readExifOrientation(file: File): Int {
        return try {
            ExifInterface(file).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } catch (error: IOException) {
            Log.w(TAG, "No EXIF orientation for ${file.name}", error)
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun applyExif(bitmap: Bitmap, orientation: Int): Bitmap {
        val transform = ExifOrientations.transformFor(orientation)
        if (transform.isIdentity) return bitmap
        val matrix = Matrix()
        if (transform.rotationDegreesClockwise != 0) {
            matrix.postRotate(transform.rotationDegreesClockwise.toFloat())
        }
        if (transform.flipHorizontalAfterRotation) {
            matrix.postScale(-1f, 1f)
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private companion object {
        const val TAG = "WallpaperDecoder"
    }
}

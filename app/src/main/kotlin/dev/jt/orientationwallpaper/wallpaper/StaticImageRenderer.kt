package dev.jt.orientationwallpaper.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.util.Log
import dev.jt.orientationwallpaper.CropMath
import dev.jt.orientationwallpaper.PixelSize
import dev.jt.orientationwallpaper.WallpaperSlot
import dev.jt.orientationwallpaper.WallpaperStore
import java.io.File
import java.util.concurrent.ExecutorService

/**
 * Center-crops one still image so it fills the surface. Call [onContentChanged]
 * on the main thread; decoding happens on [executor] and the redraw is posted back.
 */
class StaticImageRenderer(
    override val slot: WallpaperSlot,
    private val store: WallpaperStore,
    private val decoder: WallpaperBitmapDecoder,
    private val targetSize: () -> PixelSize,
    private val executor: ExecutorService,
    private val mainHandler: Handler,
    private val requestRedraw: () -> Unit,
) : OrientationRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private var bitmap: Bitmap? = null
    private var loadedKey: String? = null
    private var requestGeneration = 0

    override fun onContentChanged() {
        val file = store.imageFile(slot)
        val target = targetSize()
        val key = cacheKey(file, target, store.revision())
        if (key == loadedKey) return
        val generation = ++requestGeneration
        executor.execute {
            val decoded = decodeOrNull(file, target)
            mainHandler.post {
                if (generation != requestGeneration) {
                    decoded?.recycle()
                    return@post
                }
                val previous = bitmap
                bitmap = decoded
                loadedKey = key
                if (previous != null && previous !== decoded) previous.recycle()
                requestRedraw()
            }
        }
    }

    override fun draw(canvas: Canvas, surfaceWidth: Int, surfaceHeight: Int) {
        canvas.drawColor(EMPTY_COLOR)
        val current = bitmap
        if (current == null || current.isRecycled || surfaceWidth <= 0 || surfaceHeight <= 0) return
        val crop = CropMath.centerCrop(current.width, current.height, surfaceWidth, surfaceHeight)
        val matrix = android.graphics.Matrix().apply {
            postScale(crop.scale, crop.scale)
            postTranslate(crop.dx, crop.dy)
        }
        canvas.drawBitmap(current, matrix, paint)
    }

    override fun release() {
        requestGeneration++
        loadedKey = null
        bitmap?.recycle()
        bitmap = null
    }

    private fun decodeOrNull(file: File?, target: PixelSize): Bitmap? {
        if (file == null || !file.isFile) return null
        return try {
            decoder.decode(file, target)
        } catch (error: Exception) {
            Log.e(TAG, "Failed to decode ${file.name} for $slot", error)
            null
        }
    }

    private fun cacheKey(file: File?, target: PixelSize, revision: Long): String {
        if (file == null) return "empty:$revision:${target.width}x${target.height}"
        return listOf(
            file.absolutePath,
            file.length(),
            file.lastModified(),
            revision,
            target.width,
            target.height,
        ).joinToString(":")
    }

    private companion object {
        const val TAG = "StaticImageRenderer"
        const val EMPTY_COLOR = 0xFF101412.toInt()
    }
}

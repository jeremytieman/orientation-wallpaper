package dev.jt.orientationwallpaper.wallpaper

import android.graphics.Canvas
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import dev.jt.orientationwallpaper.DecodeTargets
import dev.jt.orientationwallpaper.WallpaperSlot
import dev.jt.orientationwallpaper.WallpaperStore
import dev.jt.orientationwallpaper.data.DisplaySizes
import dev.jt.orientationwallpaper.data.WallpaperLocations
import dev.jt.orientationwallpaper.displaySizeChanged
import dev.jt.orientationwallpaper.shouldListenForDisplayChanges
import dev.jt.orientationwallpaper.wallpaperSlot
import java.io.File
import java.util.concurrent.Executors

/**
 * Live wallpaper that draws the portrait or landscape image for the current
 * display, not the surface. Launchers can hand the engine a surface wider than
 * the screen and can keep that surface the same size across rotation, so each
 * engine also listens for display changes while it is visible. Launcher page
 * offsets are ignored so the crop stays put instead of panning. Each engine
 * owns its renderers, so the picker preview and the real wallpaper do not
 * share bitmaps.
 */
class OrientationWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = OrientationEngine()

    private inner class OrientationEngine : Engine() {
        private val directory = WallpaperLocations.directory(applicationContext)
        private val store = WallpaperStore(directory)
        private val decoder = WallpaperBitmapDecoder()
        private val mainHandler = Handler(Looper.getMainLooper())
        private val executor = Executors.newSingleThreadExecutor()
        private val observer = watchDirectory(directory) {
            mainHandler.post { onImagesChanged() }
        }
        private var displayWidth = 0
        private var displayHeight = 0
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var surfaceReady = false
        private var visible = false
        private var destroyed = false
        private var listeningForDisplay = false
        private val displayManager = applicationContext.getSystemService(DisplayManager::class.java)
        private val displayListener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = onDisplayMetricsChanged()

            override fun onDisplayRemoved(displayId: Int) = onDisplayMetricsChanged()

            override fun onDisplayChanged(displayId: Int) = onDisplayMetricsChanged()
        }

        private val renderers: Map<WallpaperSlot, OrientationRenderer> = WallpaperSlot.entries.associateWith { slot ->
            StaticImageRenderer(
                slot = slot,
                store = store,
                decoder = decoder,
                targetSize = { targetFor(slot) },
                executor = executor,
                mainHandler = mainHandler,
                requestRedraw = { requestRedraw() },
            )
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setOffsetNotificationsEnabled(false)
            directory.mkdirs()
            refreshDisplay()
            observer.startWatching()
        }

        override fun onDestroy() {
            destroyed = true
            visible = false
            updateDisplayListener()
            observer.stopWatching()
            renderers.values.forEach { it.release() }
            executor.shutdownNow()
            super.onDestroy()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            surfaceReady = true
            renderers.values.forEach { it.onContentChanged() }
            drawFrame()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceReady = true
            surfaceWidth = width
            surfaceHeight = height
            refreshDisplay()
            renderers.values.forEach { it.onContentChanged() }
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            surfaceReady = false
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            updateDisplayListener()
            if (visible) {
                refreshDisplay()
                renderers.values.forEach { it.onContentChanged() }
                drawFrame()
            }
        }

        private fun onDisplayMetricsChanged() {
            if (destroyed) return
            val previousWidth = displayWidth
            val previousHeight = displayHeight
            refreshDisplay()
            if (!displaySizeChanged(previousWidth, previousHeight, displayWidth, displayHeight)) return
            renderers.values.forEach { it.onContentChanged() }
            requestRedraw()
        }

        private fun updateDisplayListener() {
            val shouldListen = shouldListenForDisplayChanges(visible, destroyed)
            if (shouldListen == listeningForDisplay) return
            val manager = displayManager ?: return
            if (shouldListen) {
                manager.registerDisplayListener(displayListener, mainHandler)
                listeningForDisplay = true
            } else {
                manager.unregisterDisplayListener(displayListener)
                listeningForDisplay = false
            }
        }

        private fun onImagesChanged() {
            if (destroyed) return
            if (visible || isPreview) {
                renderers.values.forEach { it.onContentChanged() }
            }
        }

        private fun requestRedraw() {
            if (!destroyed && surfaceReady && (visible || isPreview)) drawFrame()
        }

        private fun targetFor(slot: WallpaperSlot) = DecodeTargets.forSlot(
            slot = slot,
            displayWidth = displayWidth,
            displayHeight = displayHeight,
            surfaceWidth = surfaceWidth,
            surfaceHeight = surfaceHeight,
            preview = isPreview,
        )

        private fun refreshDisplay() {
            val size = DisplaySizes.pixels(applicationContext)
            displayWidth = size.width
            displayHeight = size.height
        }

        private fun drawFrame() {
            if (!surfaceReady || destroyed) return
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: return
                val width = canvas.width
                val height = canvas.height
                if (width <= 0 || height <= 0) return
                refreshDisplay()
                val slot = wallpaperSlot(displayWidth, displayHeight, width, height)
                renderers.getValue(slot).draw(canvas, width, height)
            } catch (error: Exception) {
                Log.e(TAG, "draw failed", error)
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (error: Exception) {
                        Log.w(TAG, "unlock failed", error)
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "OrientWallpaper"
    }
}

@Suppress("DEPRECATION")
private fun watchDirectory(directory: File, onChange: () -> Unit): FileObserver {
    val mask = FileObserver.CLOSE_WRITE or
        FileObserver.MOVED_TO or
        FileObserver.MOVED_FROM or
        FileObserver.DELETE or
        FileObserver.CREATE
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        object : FileObserver(directory, mask) {
            override fun onEvent(event: Int, path: String?) {
                onChange()
            }
        }
    } else {
        object : FileObserver(directory.absolutePath, mask) {
            override fun onEvent(event: Int, path: String?) {
                onChange()
            }
        }
    }
}

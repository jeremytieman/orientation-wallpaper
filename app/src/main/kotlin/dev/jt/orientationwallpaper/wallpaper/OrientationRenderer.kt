package dev.jt.orientationwallpaper.wallpaper

import android.graphics.Canvas
import dev.jt.orientationwallpaper.WallpaperSlot

/**
 * Draws one orientation of the wallpaper.
 *
 * The engine selects an implementation from the surface size. A later animated
 * orientation can replace [StaticImageRenderer] for one slot; the engine keeps
 * asking the selected renderer to draw.
 */
interface OrientationRenderer {
    val slot: WallpaperSlot

    fun draw(canvas: Canvas, surfaceWidth: Int, surfaceHeight: Int)

    /** The stored image, or the size it should be decoded at, may have changed. */
    fun onContentChanged()

    fun release()
}

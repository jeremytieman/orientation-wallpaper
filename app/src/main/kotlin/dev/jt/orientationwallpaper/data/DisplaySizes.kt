package dev.jt.orientationwallpaper.data

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import dev.jt.orientationwallpaper.PixelSize

object DisplaySizes {
    fun pixels(context: Context): PixelSize {
        val windowManager = context.getSystemService(WindowManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.maximumWindowMetrics.bounds
            PixelSize(bounds.width(), bounds.height())
        } else {
            val point = Point()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(point)
            PixelSize(point.x, point.y)
        }
    }
}

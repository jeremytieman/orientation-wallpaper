package dev.jt.orientationwallpaper.wallpaper

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WallpaperChooser {
    fun launch(context: Context): Boolean {
        val component = ComponentName(context, OrientationWallpaperService::class.java)
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        }
        if (start(context, direct)) return true
        return start(context, Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
    }

    private fun start(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (missing: ActivityNotFoundException) {
            false
        }
    }
}

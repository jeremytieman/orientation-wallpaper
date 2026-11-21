package dev.jt.orientationwallpaper.data

import android.content.Context
import java.io.File

/**
 * Device-protected storage so the wallpaper service can read the images before
 * the user unlocks the device. The activity and the service must both use this
 * directory; the default credential-encrypted files dir is a different place.
 */
object WallpaperLocations {
    fun directory(context: Context): File {
        val storage = context.createDeviceProtectedStorageContext()
        return File(storage.filesDir, "wallpapers")
    }
}

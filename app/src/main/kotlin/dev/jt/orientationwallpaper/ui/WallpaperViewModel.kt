package dev.jt.orientationwallpaper.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.jt.orientationwallpaper.ImageFormats
import dev.jt.orientationwallpaper.PixelSize
import dev.jt.orientationwallpaper.UnsupportedImageFormatException
import dev.jt.orientationwallpaper.WallpaperSlot
import dev.jt.orientationwallpaper.WallpaperStore
import dev.jt.orientationwallpaper.data.WallpaperLocations
import dev.jt.orientationwallpaper.wallpaper.WallpaperBitmapDecoder
import java.io.FileNotFoundException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class ImportTarget {
    PORTRAIT,
    LANDSCAPE,
    BOTH,
}

enum class UserMessage {
    UNSUPPORTED,
    COULD_NOT_OPEN,
    DECODE_FAILED,
}

data class SlotUi(
    val formatLabel: String? = null,
    val preview: Bitmap? = null,
)

data class WallpaperUiState(
    val portrait: SlotUi = SlotUi(),
    val landscape: SlotUi = SlotUi(),
    val busy: Boolean = false,
    val message: UserMessage? = null,
) {
    val ready: Boolean get() = portrait.preview != null && landscape.preview != null
}

class WallpaperViewModel(application: Application) : AndroidViewModel(application) {
    private val store = WallpaperStore(WallpaperLocations.directory(application))
    private val decoder = WallpaperBitmapDecoder()
    private val gate = Mutex()
    private val ui = MutableStateFlow(WallpaperUiState())

    val state: StateFlow<WallpaperUiState> = ui.asStateFlow()

    init {
        viewModelScope.launch {
            gate.withLock { publish(message = null) }
        }
    }

    fun import(target: ImportTarget, uri: Uri) {
        viewModelScope.launch {
            gate.withLock {
                ui.update { it.copy(busy = true, message = null) }
                val message = withContext(Dispatchers.IO) { importBlocking(target, uri) }
                publish(message)
            }
        }
    }

    fun messageShown() {
        ui.update { state -> if (state.message == null) state else state.copy(message = null) }
    }

    private fun importBlocking(target: ImportTarget, uri: Uri): UserMessage? {
        return try {
            val opened = getApplication<Application>().contentResolver.openInputStream(uri)
            if (opened == null) {
                UserMessage.COULD_NOT_OPEN
            } else {
                opened.use { input ->
                    when (target) {
                        ImportTarget.PORTRAIT -> importSlot(WallpaperSlot.PORTRAIT, input)
                        ImportTarget.LANDSCAPE -> importSlot(WallpaperSlot.LANDSCAPE, input)
                        ImportTarget.BOTH -> importBoth(input)
                    }
                }
                null
            }
        } catch (unsupported: UnsupportedImageFormatException) {
            UserMessage.UNSUPPORTED
        } catch (missing: FileNotFoundException) {
            UserMessage.COULD_NOT_OPEN
        } catch (error: IOException) {
            Log.e(TAG, "import failed", error)
            UserMessage.DECODE_FAILED
        } catch (error: Exception) {
            Log.e(TAG, "import failed", error)
            UserMessage.DECODE_FAILED
        }
    }

    private fun importSlot(slot: WallpaperSlot, input: java.io.InputStream) {
        val staged = store.stage(input)
        try {
            decoder.decode(staged.file, PixelSize(48, 48)).recycle()
            store.commit(slot, staged)
        } finally {
            store.discard(staged)
        }
    }

    private fun importBoth(input: java.io.InputStream) {
        val staged = store.stage(input)
        try {
            decoder.decode(staged.file, PixelSize(48, 48)).recycle()
            store.commitBoth(staged)
        } finally {
            store.discard(staged)
        }
    }

    private suspend fun publish(message: UserMessage?) {
        val portrait = withContext(Dispatchers.IO) { loadSlot(WallpaperSlot.PORTRAIT) }
        val landscape = withContext(Dispatchers.IO) { loadSlot(WallpaperSlot.LANDSCAPE) }
        ui.value = WallpaperUiState(
            portrait = portrait,
            landscape = landscape,
            busy = false,
            message = message,
        )
    }

    private fun loadSlot(slot: WallpaperSlot): SlotUi {
        val file = store.imageFile(slot) ?: return SlotUi()
        val label = ImageFormats.fromFileName(file.name)?.label
        val preview = try {
            val target = if (slot == WallpaperSlot.PORTRAIT) {
                PixelSize(720, 960)
            } else {
                PixelSize(960, 720)
            }
            decoder.decode(file, target)
        } catch (error: Exception) {
            Log.w(TAG, "preview failed for $slot", error)
            null
        }
        return SlotUi(formatLabel = label, preview = preview)
    }

    private companion object {
        const val TAG = "WallpaperViewModel"
    }
}

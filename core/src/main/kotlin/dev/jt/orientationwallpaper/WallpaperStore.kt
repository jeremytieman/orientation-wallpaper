package dev.jt.orientationwallpaper

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.Properties

class UnsupportedImageFormatException : Exception("Unsupported image format")

/**
 * Copies picked images into an app-private directory and remembers which file
 * belongs to each orientation. Callers pass the bytes; this class does not know
 * about Android content URIs, so the copy survives deletion of the source.
 */
class WallpaperStore(val directory: File) {
    private val lock = Any()

    init {
        directory.mkdirs()
    }

    fun revision(): Long = synchronized(lock) { readState().revision }

    fun imageFile(slot: WallpaperSlot): File? = synchronized(lock) {
        val name = readState().name(slot) ?: return null
        if (!isSafeName(name)) return null
        File(directory, name).takeIf { it.isFile }
    }

    fun import(slot: WallpaperSlot, input: InputStream): ImageFormat = synchronized(lock) {
        val staged = stage(input)
        try {
            val format = identifyOrThrow(staged)
            val dest = publish(staged, fileName(slot, format))
            deleteSiblings(slot, keep = dest.name)
            writeState(readState().with(slot, dest.name).bump())
            format
        } finally {
            if (staged.exists()) staged.delete()
        }
    }

    fun importBoth(input: InputStream): ImageFormat = synchronized(lock) {
        val staged = stage(input)
        try {
            val format = identifyOrThrow(staged)
            val portrait = publish(staged, fileName(WallpaperSlot.PORTRAIT, format))
            val landscape = publish(staged, fileName(WallpaperSlot.LANDSCAPE, format))
            deleteSiblings(WallpaperSlot.PORTRAIT, keep = portrait.name)
            deleteSiblings(WallpaperSlot.LANDSCAPE, keep = landscape.name)
            writeState(
                WallpaperState(
                    revision = readState().revision + 1,
                    portrait = portrait.name,
                    landscape = landscape.name,
                ),
            )
            format
        } finally {
            if (staged.exists()) staged.delete()
        }
    }

    fun clear(slot: WallpaperSlot) = synchronized(lock) {
        deleteSiblings(slot, keep = null)
        writeState(readState().with(slot, null).bump())
    }

    private fun stage(input: InputStream): File {
        directory.mkdirs()
        val temp = File(directory, ".import-${System.nanoTime()}.tmp")
        temp.outputStream().use { output -> input.copyTo(output) }
        if (temp.length() <= 0L) {
            temp.delete()
            throw UnsupportedImageFormatException()
        }
        return temp
    }

    private fun identifyOrThrow(file: File): ImageFormat {
        val header = file.inputStream().use { input ->
            val buffer = ByteArray(64)
            val read = input.read(buffer)
            if (read <= 0) ByteArray(0) else buffer.copyOf(read)
        }
        return ImageFormats.identify(header) ?: throw UnsupportedImageFormatException()
    }

    private fun publish(source: File, destName: String): File {
        val dest = File(directory, destName)
        val incoming = File(directory, ".$destName.new")
        source.copyTo(incoming, overwrite = true)
        if (!incoming.renameTo(dest)) {
            incoming.copyTo(dest, overwrite = true)
            incoming.delete()
        }
        return dest
    }

    private fun deleteSiblings(slot: WallpaperSlot, keep: String?) {
        val prefix = "${slot.filePrefix}."
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(prefix) && file.name != keep) {
                file.delete()
            }
        }
    }

    private fun fileName(slot: WallpaperSlot, format: ImageFormat): String {
        return "${slot.filePrefix}.${format.extension}"
    }

    private fun readState(): WallpaperState {
        val file = File(directory, STATE_FILE)
        if (!file.isFile) return WallpaperState(revision = 0, portrait = null, landscape = null)
        return try {
            val properties = Properties()
            file.inputStream().use { properties.load(it) }
            WallpaperState(
                revision = properties.getProperty("revision")?.toLongOrNull() ?: 0L,
                portrait = properties.getProperty("portrait"),
                landscape = properties.getProperty("landscape"),
            )
        } catch (error: IOException) {
            WallpaperState(revision = 0, portrait = null, landscape = null)
        }
    }

    private fun writeState(state: WallpaperState) {
        directory.mkdirs()
        val properties = Properties()
        properties.setProperty("revision", state.revision.toString())
        state.portrait?.let { properties.setProperty("portrait", it) }
        state.landscape?.let { properties.setProperty("landscape", it) }
        val temp = File(directory, ".$STATE_FILE.new")
        temp.outputStream().use { properties.store(it, null) }
        val dest = File(directory, STATE_FILE)
        if (!temp.renameTo(dest)) {
            temp.copyTo(dest, overwrite = true)
            temp.delete()
        }
    }

    private fun isSafeName(name: String): Boolean {
        return name.isNotBlank() &&
            name == File(name).name &&
            !name.contains('/') &&
            !name.contains('\\') &&
            !name.contains("..") &&
            !name.startsWith(".")
    }

    private data class WallpaperState(
        val revision: Long,
        val portrait: String?,
        val landscape: String?,
    ) {
        fun name(slot: WallpaperSlot): String? = when (slot) {
            WallpaperSlot.PORTRAIT -> portrait
            WallpaperSlot.LANDSCAPE -> landscape
        }

        fun with(slot: WallpaperSlot, fileName: String?): WallpaperState = when (slot) {
            WallpaperSlot.PORTRAIT -> copy(portrait = fileName)
            WallpaperSlot.LANDSCAPE -> copy(landscape = fileName)
        }

        fun bump(): WallpaperState = copy(revision = revision + 1)
    }

    private companion object {
        const val STATE_FILE = "state.properties"
    }
}

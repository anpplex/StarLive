package com.starlive.app.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Local multi-image library (imported customs). Max [MAX_ITEMS].
 * Active selection still uses [WallpaperRepository.activeFile].
 */
object WallpaperLibrary {
    private const val TAG = "StarLive"
    private const val INDEX = "index.json"
    const val MAX_ITEMS = 24
    private const val DIR = "library"

    data class Item(val id: String, val label: String, val fileName: String) {
        fun file(context: Context): File = File(dir(context), fileName)
    }

    fun dir(context: Context): File =
        File(context.applicationContext.filesDir, DIR).also { it.mkdirs() }

    fun list(context: Context): List<Item> {
        val f = File(dir(context), INDEX)
        if (!f.isFile) return emptyList()
        return runCatching {
            LibraryIndexCodec.decode(f.readText()).filter { item ->
                File(dir(context), item.fileName).isFile
            }
        }.onFailure { Log.w(TAG, "library list failed", it) }.getOrDefault(emptyList())
    }

    /** Save bitmap into library and set as active custom. Returns item id. */
    fun addAndActivate(context: Context, bitmap: Bitmap, label: String): String? {
        return runCatching {
            val app = context.applicationContext
            val id = "lib_${System.currentTimeMillis()}"
            val fileName = "$id.jpg"
            val outFile = File(dir(app), fileName)
            FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            // copy to active
            outFile.copyTo(WallpaperRepository.activeFile(app), overwrite = true)
            WallpaperRepository.prefs(app).edit()
                .putBoolean("has_image", true)
                .putString("active_id", "lib:$id")
                .putString("custom_label", label)
                .apply()
            val items = list(app).toMutableList()
            items.add(0, Item(id, label, fileName))
            while (items.size > MAX_ITEMS) {
                val drop = items.removeAt(items.lastIndex)
                drop.file(app).delete()
            }
            writeIndex(app, items)
            id
        }.onFailure { Log.w(TAG, "library add failed", it) }.getOrNull()
    }

    fun apply(context: Context, id: String): Boolean {
        val item = list(context).firstOrNull { it.id == id } ?: return false
        val src = item.file(context)
        if (!src.isFile) return false
        return runCatching {
            src.copyTo(WallpaperRepository.activeFile(context), overwrite = true)
            WallpaperRepository.prefs(context).edit()
                .putBoolean("has_image", true)
                .putString("active_id", "lib:$id")
                .putString("custom_label", item.label)
                .apply()
            true
        }.getOrDefault(false)
    }

    fun delete(context: Context, id: String): Boolean {
        val items = list(context).toMutableList()
        val idx = items.indexOfFirst { it.id == id }
        if (idx < 0) return false
        val removed = items.removeAt(idx)
        removed.file(context).delete()
        writeIndex(context, items)
        // If deleted was active: fall back to first remaining lib item or demo
        val active = WallpaperRepository.activeId(context)
        if (active == "lib:$id" || active == id) {
            val next = items.firstOrNull()
            if (next != null) {
                apply(context, next.id)
            } else {
                WallpaperRepository.restoreDemo(context)
            }
        }
        return true
    }

    private fun writeIndex(context: Context, items: List<Item>) {
        File(dir(context), INDEX).writeText(LibraryIndexCodec.encode(items))
    }
}


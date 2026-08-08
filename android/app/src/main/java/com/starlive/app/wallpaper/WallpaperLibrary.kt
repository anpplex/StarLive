package com.starlive.app.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
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

    data class Item(
        val id: String,
        val label: String,
        val fileName: String,
        val kind: String = AnimatedMedia.KIND_STATIC,
        val cropL: Float = 0f,
        val cropT: Float = 0f,
        val cropR: Float = 0f,
        val cropB: Float = 0f,
    ) {
        fun file(context: Context): File = File(dir(context), fileName)

        /** Crop is valid when the rect has positive width and height. */
        fun hasCrop(): Boolean = cropR > cropL && cropB > cropT

        fun cropRect(): RectF? =
            if (hasCrop()) RectF(cropL, cropT, cropR, cropB) else null
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

    /** Save bitmap into library and set as active custom (static JPEG). Returns item id. */
    fun addAndActivate(context: Context, bitmap: Bitmap, label: String): String? {
        return runCatching {
            val app = context.applicationContext
            val id = "lib_${System.currentTimeMillis()}"
            val fileName = "$id.jpg"
            val outFile = File(dir(app), fileName)
            FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            WallpaperRepository.activateMedia(
                app,
                outFile,
                kind = AnimatedMedia.KIND_STATIC,
                cropRect = null,
                activeId = "lib:$id",
                label = label,
            )
            val items = list(app).toMutableList()
            items.add(0, Item(id, label, fileName, kind = AnimatedMedia.KIND_STATIC))
            trimAndWrite(app, items)
            id
        }.onFailure { Log.w(TAG, "library add failed", it) }.getOrNull()
    }

    /**
     * Save animated GIF/WebP bytes into library, copy to active, persist kind + crop.
     * Returns item id, or null on failure / unsupported kind.
     */
    fun addAndActivateAnimated(
        context: Context,
        bytes: ByteArray,
        mimeOrKind: String,
        label: String,
        cropRect: RectF? = null,
    ): String? {
        return runCatching {
            val app = context.applicationContext
            val kind = AnimatedMedia.kindFromMimeOrBytes(mimeOrKind, bytes)
            if (!AnimatedMedia.isAnimatedKind(kind)) {
                Log.w(TAG, "library add animated: not animated kind=$kind mime=$mimeOrKind")
                return@runCatching null
            }
            if (bytes.size < 32) return@runCatching null
            val id = "lib_${System.currentTimeMillis()}"
            val ext = AnimatedMedia.extensionForKind(kind)
            val fileName = "$id.$ext"
            val outFile = File(dir(app), fileName)
            outFile.writeBytes(bytes)
            val crop = cropRect?.takeIf { it.right > it.left && it.bottom > it.top }
            WallpaperRepository.activateMedia(
                app,
                outFile,
                kind = kind,
                cropRect = crop,
                activeId = "lib:$id",
                label = label,
            )
            val item = Item(
                id = id,
                label = label,
                fileName = fileName,
                kind = kind,
                cropL = crop?.left ?: 0f,
                cropT = crop?.top ?: 0f,
                cropR = crop?.right ?: 0f,
                cropB = crop?.bottom ?: 0f,
            )
            val items = list(app).toMutableList()
            items.add(0, item)
            trimAndWrite(app, items)
            id
        }.onFailure { Log.w(TAG, "library add animated failed", it) }.getOrNull()
    }

    fun apply(context: Context, id: String): Boolean {
        val item = list(context).firstOrNull { it.id == id } ?: return false
        val src = item.file(context)
        if (!src.isFile) return false
        return runCatching {
            WallpaperRepository.activateMedia(
                context,
                src,
                kind = item.kind.ifBlank { AnimatedMedia.KIND_STATIC },
                cropRect = item.cropRect(),
                activeId = "lib:$id",
                label = item.label,
            )
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

    private fun trimAndWrite(context: Context, items: MutableList<Item>) {
        while (items.size > MAX_ITEMS) {
            val drop = items.removeAt(items.lastIndex)
            drop.file(context).delete()
        }
        writeIndex(context, items)
    }

    private fun writeIndex(context: Context, items: List<Item>) {
        File(dir(context), INDEX).writeText(LibraryIndexCodec.encode(items))
    }
}

package com.starlive.app.handoff

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.starlive.app.BuildConfig
import com.starlive.app.wallpaper.WallpaperRepository
import java.io.File

/**
 * Read-only handoff for future Lyra integration.
 *
 * content://com.starlive.app.handoff/active  → JPEG file
 * content://com.starlive.app.handoff/meta    → one-row cursor
 */
class HandoffProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? = when (MATCHER.match(uri)) {
        CODE_ACTIVE -> "image/jpeg"
        CODE_META -> "vnd.android.cursor.item/vnd.com.starlive.handoff"
        else -> null
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (MATCHER.match(uri) != CODE_ACTIVE) return null
        if (mode != "r") return null
        val ctx = context ?: return null
        WallpaperRepository.ensureSeeded(ctx)
        val f = WallpaperRepository.activeFile(ctx)
        if (!f.isFile) return null
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (MATCHER.match(uri) != CODE_META) return null
        val ctx = context ?: return null
        val cols = arrayOf(
            "format", "activeId", "idlePrefer", "nightMode", "starliveVersion", "hasImage",
        )
        val c = MatrixCursor(cols)
        c.addRow(
            arrayOf(
                "starlive-handoff/v1",
                WallpaperRepository.activeId(ctx),
                if (WallpaperRepository.idlePrefer(ctx)) 1 else 0,
                WallpaperRepository.nightMode(ctx),
                BuildConfig.VERSION_NAME,
                if (WallpaperRepository.hasImage(ctx)) 1 else 0,
            ),
        )
        return c
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val AUTHORITY = "com.starlive.app.handoff"
        private const val CODE_ACTIVE = 1
        private const val CODE_META = 2
        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "active", CODE_ACTIVE)
            addURI(AUTHORITY, "meta", CODE_META)
        }

        val ACTIVE_URI: Uri = Uri.parse("content://$AUTHORITY/active")
        val META_URI: Uri = Uri.parse("content://$AUTHORITY/meta")
    }
}

package com.starlive.app.redeem

import android.content.Context
import android.util.Log
import com.starlive.ring.WallpaperCropper
import com.starlive.app.wallpaper.WallpaperRepository
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

/**
 * Install starlive theme pack zip:
 * ```
 * catalog.json  { "pack_id", "title", "wallpapers": [ { "file", "label"? } ] }
 * wallpaper.jpg (or paths listed in catalog)
 * ```
 * Default pack = 1 image; dark/light optional via multiple catalog entries.
 */
object ThemePackInstaller {
    private const val TAG = "StarLive"

    data class InstallResult(val title: String, val count: Int)

    fun installZip(context: Context, zipFile: File): Result<InstallResult> {
        return runCatching {
            ZipFile(zipFile).use { zip ->
                val catalogEntry = zip.getEntry("catalog.json")
                    ?: throw IllegalArgumentException("缺少 catalog.json")
                val catalogText = zip.getInputStream(catalogEntry).bufferedReader().readText()
                val catalog = JSONObject(catalogText)
                val title = catalog.optString("title", catalog.optString("pack_id", "主题包"))
                val arr = catalog.getJSONArray("wallpapers")
                var count = 0
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    val rel = item.getString("file")
                    val label = item.optString("label", title)
                    val entry = zip.getEntry(rel)
                        ?: zip.getEntry(rel.trimStart('/'))
                        ?: throw IllegalArgumentException("缺少文件 $rel")
                    val bytes = zip.getInputStream(entry).readBytes()
                    val cropped = WallpaperCropper.decodeAndCrop(bytes)
                        ?: throw IllegalArgumentException("无法解码 $rel")
                    if (!WallpaperRepository.commitCropped(context, cropped.bitmap, label)) {
                        throw IllegalStateException("入库失败 $label")
                    }
                    count++
                }
                if (count == 0) throw IllegalArgumentException("主题包为空")
                InstallResult(title, count)
            }
        }.onFailure { Log.w(TAG, "installZip failed", it) }
    }
}

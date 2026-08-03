package com.starlive.app.wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.starlive.app.display.StripGeometry
import com.starlive.app.display.WallpaperEdgeSoftener
import com.starlive.app.runtime.PendingApplyStore
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * Demo catalog + active wallpaper file. Keys namespaced for Lyra handoff safety.
 */
object WallpaperRepository {
    private const val TAG = "StarLive"
    private const val PREFS = "starlive_wallpaper"
    private const val KEY_IDLE = "idle_prefer"
    private const val KEY_ACTIVE_ID = "active_id"
    private const val KEY_HAS_IMAGE = "has_image"
    private const val KEY_NIGHT = "night_mode"
    private const val KEY_HINT = "first_run_hint_shown"
    private const val KEY_CUSTOM_LABEL = "custom_label"
    private const val KEY_CAROUSEL = "carousel_enabled"
    private const val KEY_CAROUSEL_MIN = "carousel_interval_min"
    private const val KEY_YIELD_LYRA = "yield_when_lyra_installed"
    private const val KEY_NLS_HINT = "nls_hint_shown"
    private const val KEY_BATTERY_HINT = "battery_hint_shown"
    private const val FILE_ACTIVE = "active_wallpaper.jpg"
    private const val CATALOG = "wallpaper/catalog.json"
    const val CAROUSEL_MIN = 1
    const val CAROUSEL_MAX = 60
    const val CAROUSEL_DEFAULT = 5

    val DOWNLOAD_CANDIDATES = listOf(
        "starlive_wallpaper.jpg",
        "starlive_wallpaper.png",
        "lyra_wallpaper.jpg",
        "lyra_wallpaper.png",
        "cluster_wallpaper.jpg",
        "cluster_wallpaper.png",
        "lyra_cluster_wallpaper.jpg",
        "lyra_cluster_wallpaper.png",
    )

    data class Demo(
        val id: String,
        val label: String,
        val darkAsset: String,
        val lightAsset: String,
    ) {
        fun assetFor(nightish: Boolean): String = if (nightish) darkAsset else lightAsset
    }

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun idlePrefer(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IDLE, true)

    fun setIdlePrefer(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_IDLE, value).apply()
    }

    fun nightMode(context: Context): String =
        prefs(context).getString(KEY_NIGHT, "auto") ?: "auto"

    fun setNightMode(context: Context, mode: String) {
        prefs(context).edit().putString(KEY_NIGHT, mode).apply()
        val id = activeId(context)
        if (id != "custom") {
            applyDemo(context, id)
        }
    }

    fun activeId(context: Context): String {
        val raw = prefs(context).getString(KEY_ACTIVE_ID, "minimal") ?: "minimal"
        return migrateLegacyId(raw)
    }

    private fun migrateLegacyId(id: String): String = when (id) {
        "demo_minimal_dark", "demo_minimal_light" -> "minimal"
        "demo_atmosphere", "demo_atmosphere_dark", "demo_atmosphere_light" -> "atmosphere"
        "demo_abstract", "demo_abstract_dark", "demo_abstract_light" -> "abstract"
        else -> id
    }

    fun firstRunHintShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HINT, false)

    fun setFirstRunHintShown(context: Context) {
        prefs(context).edit().putBoolean(KEY_HINT, true).apply()
    }

    fun isCarouselEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_CAROUSEL, false)

    fun setCarouselEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_CAROUSEL, value).apply()
    }

    fun carouselIntervalMinutes(context: Context): Int =
        prefs(context).getInt(KEY_CAROUSEL_MIN, CAROUSEL_DEFAULT)
            .coerceIn(CAROUSEL_MIN, CAROUSEL_MAX)

    fun setCarouselIntervalMinutes(context: Context, minutes: Int) {
        prefs(context).edit()
            .putInt(KEY_CAROUSEL_MIN, minutes.coerceIn(CAROUSEL_MIN, CAROUSEL_MAX))
            .apply()
    }

    fun yieldWhenLyraInstalled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_YIELD_LYRA, true)

    fun setYieldWhenLyraInstalled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_YIELD_LYRA, value).apply()
    }

    fun nlsHintShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NLS_HINT, false)

    fun setNlsHintShown(context: Context) {
        prefs(context).edit().putBoolean(KEY_NLS_HINT, true).apply()
    }

    fun batteryHintShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BATTERY_HINT, false)

    fun setBatteryHintShown(context: Context) {
        prefs(context).edit().putBoolean(KEY_BATTERY_HINT, true).apply()
    }

    fun advanceToNextDemo(context: Context): String? {
        val list = demos(context)
        if (list.size < 2) return null
        val cur = activeId(context)
        val idx = list.indexOfFirst { it.id == cur }.let { if (it < 0) 0 else it }
        val next = list[(idx + 1) % list.size]
        return if (applyDemo(context, next.id)) next.id else null
    }

    fun activeFile(context: Context): File =
        File(context.applicationContext.filesDir, FILE_ACTIVE)

    fun demos(context: Context): List<Demo> = loadCatalog(context)

    fun hasImage(context: Context): Boolean {
        ensureSeeded(context)
        val f = activeFile(context)
        return f.isFile && f.length() > 32L
    }

    fun ensureSeeded(context: Context) {
        val f = activeFile(context)
        if (f.isFile && f.length() > 32L) return
        val id = activeId(context)
        if (!applyDemo(context, id)) {
            demos(context).firstOrNull()?.let { applyDemo(context, it.id) }
        }
    }

    fun applyDemo(context: Context, id: String): Boolean {
        val themeId = migrateLegacyId(id)
        val demo = demos(context).firstOrNull { it.id == themeId } ?: return false
        val asset = demo.assetFor(isNightish(context))
        return runCatching {
            context.assets.open(asset).use { input ->
                FileOutputStream(activeFile(context)).use { out -> input.copyTo(out) }
            }
            prefs(context).edit()
                .putBoolean(KEY_HAS_IMAGE, true)
                .putString(KEY_ACTIVE_ID, themeId)
                .apply()
            true
        }.onFailure { Log.w(TAG, "applyDemo failed $themeId $asset", it) }.getOrDefault(false)
    }

    fun labelForActive(context: Context): String {
        val id = activeId(context)
        if (id.startsWith("lib:")) {
            val libId = id.removePrefix("lib:")
            val item = WallpaperLibrary.list(context).firstOrNull { it.id == libId }
            return item?.label?.let { "库 · $it" } ?: "自定义"
        }
        if (id == "custom") {
            val custom = prefs(context).getString(KEY_CUSTOM_LABEL, null)
            return if (custom.isNullOrBlank()) "自定义" else "自定义 · $custom"
        }
        return demos(context).firstOrNull { it.id == id }?.label ?: id
    }

    fun commitCropped(context: Context, bitmap: Bitmap, label: String = "导入"): Boolean {
        if (WallpaperLibrary.addAndActivate(context, bitmap, label) != null) return true
        return runCatching {
            val app = context.applicationContext
            FileOutputStream(activeFile(app)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            prefs(app).edit()
                .putBoolean(KEY_HAS_IMAGE, true)
                .putString(KEY_ACTIVE_ID, "custom")
                .putString(KEY_CUSTOM_LABEL, label)
                .apply()
            true
        }.onFailure { Log.w(TAG, "commitCropped failed", it) }.getOrDefault(false)
    }

    fun applyLibraryItem(context: Context, libId: String): Boolean =
        WallpaperLibrary.apply(context, libId)

    fun libraryItems(context: Context): List<WallpaperLibrary.Item> =
        WallpaperLibrary.list(context)

    fun deleteLibraryItem(context: Context, libId: String): Boolean =
        WallpaperLibrary.delete(context, libId)

    fun findDownloadCandidate(context: Context): File? {
        val roots = listOfNotNull(
            File("/sdcard/Download/StarLive"),
            File("/storage/emulated/0/Download/StarLive"),
            File("/sdcard/Download"),
            File("/storage/emulated/0/Download"),
            File("/sdcard/Pictures"),
            File("/storage/emulated/0/Pictures"),
            context.getExternalFilesDir(null),
        )
        for (root in roots) {
            if (!root.isDirectory) continue
            for (name in DOWNLOAD_CANDIDATES) {
                val f = File(root, name)
                if (f.isFile && f.length() > 32L) return f
            }
        }
        return null
    }

    fun restoreDemo(context: Context): Boolean {
        PendingApplyStore.clear(context)
        val first = demos(context).firstOrNull()?.id ?: "minimal"
        prefs(context).edit().remove(KEY_CUSTOM_LABEL).apply()
        return applyDemo(context, first)
    }

    fun exportHandoffFiles(context: Context, versionName: String): Boolean {
        return try {
            ensureSeeded(context)
            val active = activeFile(context)
            if (!active.isFile) return false
            val starliveDir = File("/storage/emulated/0/Download/StarLive")
            starliveDir.mkdirs()
            val downloadRoot = File("/storage/emulated/0/Download")
            downloadRoot.mkdirs()
            active.copyTo(File(starliveDir, "active_wallpaper.jpg"), overwrite = true)
            active.copyTo(File(starliveDir, "starlive_wallpaper.jpg"), overwrite = true)
            active.copyTo(File(starliveDir, "lyra_wallpaper.jpg"), overwrite = true)
            active.copyTo(File(downloadRoot, "starlive_wallpaper.jpg"), overwrite = true)
            active.copyTo(File(downloadRoot, "lyra_wallpaper.jpg"), overwrite = true)
            val json = buildString {
                append("{\n")
                append("  \"format\": \"starlive-handoff/v1\",\n")
                append("  \"activeId\": \"${activeId(context)}\",\n")
                append("  \"idlePrefer\": ${idlePrefer(context)},\n")
                append("  \"nightMode\": \"${nightMode(context)}\",\n")
                append("  \"starliveVersion\": \"$versionName\",\n")
                append("  \"files\": {\n")
                append("    \"active\": \"active_wallpaper.jpg\",\n")
                append("    \"lyra_wallpaper\": \"lyra_wallpaper.jpg\"\n")
                append("  }\n")
                append("}\n")
            }
            File(starliveDir, "handoff.json").writeText(json)
            true
        } catch (e: Exception) {
            Log.w(TAG, "exportHandoff failed", e)
            false
        }
    }

    fun decodeActiveForStrip(context: Context, nightish: Boolean): Bitmap? {
        ensureSeeded(context)
        val f = activeFile(context)
        if (!f.isFile) return null
        val opts = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeFile(f.absolutePath, opts) ?: return null
        val scaled = if (raw.width != StripGeometry.WALLPAPER_W || raw.height != StripGeometry.WALLPAPER_H) {
            Bitmap.createScaledBitmap(raw, StripGeometry.WALLPAPER_W, StripGeometry.WALLPAPER_H, true).also {
                if (it !== raw) raw.recycle()
            }
        } else {
            raw
        }
        val fade = if (nightish) StripGeometry.EDGE_FEATHER_NIGHT else StripGeometry.EDGE_FEATHER_DAY
        val glass = if (nightish) StripGeometry.GLASS_NIGHT else StripGeometry.GLASS_DAY
        return WallpaperEdgeSoftener.softenLeftEdge(scaled, fade, glass)
    }

    fun isNightish(context: Context): Boolean {
        return when (nightMode(context)) {
            "dark" -> true
            "light" -> false
            else -> {
                val night = context.resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
                night == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    private fun loadCatalog(context: Context): List<Demo> {
        return try {
            val json = context.assets.open(CATALOG).bufferedReader().use { it.readText() }
            val arr = JSONObject(json).getJSONArray("wallpapers")
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.getString("id")
                    if (o.has("dark") && o.has("light")) {
                        add(
                            Demo(
                                id = id,
                                label = o.optString("label", id),
                                darkAsset = "wallpaper/${o.getString("dark")}",
                                lightAsset = "wallpaper/${o.getString("light")}",
                            ),
                        )
                    } else {
                        val file = o.getString("file")
                        add(
                            Demo(
                                id = id,
                                label = o.optString("label", id),
                                darkAsset = "wallpaper/$file",
                                lightAsset = "wallpaper/$file",
                            ),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "catalog load failed", e)
            listOf(
                Demo(
                    "minimal",
                    "简约",
                    "wallpaper/demo_minimal_dark.jpg",
                    "wallpaper/demo_minimal_light.jpg",
                ),
            )
        }
    }
}

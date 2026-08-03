package com.starlive.app.wallpaper

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.starlive.app.display.StripGeometry
import com.starlive.app.display.WallpaperEdgeSoftener
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
    private const val KEY_NIGHT = "night_mode" // auto|dark|light
    private const val KEY_HINT = "first_run_hint_shown"
    private const val FILE_ACTIVE = "active_wallpaper.jpg"
    private const val CATALOG = "wallpaper/catalog.json"

    data class Demo(val id: String, val assetPath: String, val label: String)

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
    }

    fun activeId(context: Context): String =
        prefs(context).getString(KEY_ACTIVE_ID, "demo_minimal_dark") ?: "demo_minimal_dark"

    fun firstRunHintShown(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HINT, false)

    fun setFirstRunHintShown(context: Context) {
        prefs(context).edit().putBoolean(KEY_HINT, true).apply()
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
        val demo = demos(context).firstOrNull { it.id == id } ?: return false
        return runCatching {
            context.assets.open(demo.assetPath).use { input ->
                FileOutputStream(activeFile(context)).use { out -> input.copyTo(out) }
            }
            prefs(context).edit()
                .putBoolean(KEY_HAS_IMAGE, true)
                .putString(KEY_ACTIVE_ID, id)
                .apply()
            true
        }.onFailure { Log.w(TAG, "applyDemo failed $id", it) }.getOrDefault(false)
    }

    fun labelForActive(context: Context): String {
        val id = activeId(context)
        if (id == "custom") return "自定义"
        return demos(context).firstOrNull { it.id == id }?.label ?: id
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
        return runCatching {
            val json = context.assets.open(CATALOG).bufferedReader().use { it.readText() }
            val arr = JSONObject(json).getJSONArray("wallpapers")
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Demo(
                            id = o.getString("id"),
                            assetPath = "wallpaper/${o.getString("file")}",
                            label = o.optString("label", o.getString("id")),
                        ),
                    )
                }
            }
        }.onFailure { Log.w(TAG, "catalog load failed", it) }
            .getOrElse {
                listOf(Demo("demo_minimal_dark", "wallpaper/demo_minimal_dark.jpg", "简约深"))
            }
    }
}

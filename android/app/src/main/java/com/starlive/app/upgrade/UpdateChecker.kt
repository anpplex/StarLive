package com.starlive.app.upgrade

import android.util.Log
import com.starlive.app.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Optional GitHub Releases check (no crash offline). */
object UpdateChecker {
    private const val TAG = "StarLive"
    private const val LATEST =
        "https://api.github.com/repos/anpplex/StarLive/releases/latest"

    data class Release(
        val tag: String,
        val name: String,
        val htmlUrl: String,
        val isNewer: Boolean,
    )

    fun fetchLatest(): Result<Release> {
        return runCatching {
            val conn = (URL(LATEST).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "StarLive/${BuildConfig.VERSION_NAME}")
            }
            try {
                if (conn.responseCode !in 200..299) {
                    throw RuntimeException("检查更新失败 HTTP ${conn.responseCode}")
                }
                val text = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(text)
                val tag = json.optString("tag_name", "").ifBlank {
                    throw RuntimeException("无版本信息")
                }
                val name = json.optString("name", tag)
                val url = json.optString(
                    "html_url",
                    "https://github.com/anpplex/StarLive/releases",
                )
                Release(
                    tag = tag,
                    name = name,
                    htmlUrl = url,
                    isNewer = isNewerThan(BuildConfig.VERSION_NAME, tag),
                )
            } finally {
                conn.disconnect()
            }
        }.onFailure { Log.w(TAG, "update check failed", it) }
    }

    /**
     * Compare app versionName (e.g. 0.1.8-polish) with release tag (v0.1.7-core).
     * Uses leading numeric segments only.
     */
    fun isNewerThan(current: String, remoteTag: String): Boolean {
        val a = numericParts(current)
        val b = numericParts(remoteTag)
        val n = maxOf(a.size, b.size)
        for (i in 0 until n) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (y > x) return true
            if (y < x) return false
        }
        return false
    }

    private fun numericParts(raw: String): List<Int> {
        val s = raw.trim().removePrefix("v").removePrefix("V")
        val head = s.takeWhile { it.isDigit() || it == '.' }
        if (head.isBlank()) return listOf(0)
        return head.split('.').mapNotNull { it.toIntOrNull() }
    }
}

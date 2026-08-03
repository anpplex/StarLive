package com.starlive.app.contact

import android.content.Context
import android.util.Log

/**
 * Author / order contact. Assets under [assets/contact/] can override defaults.
 *
 * Defaults match public channels: 微信 anpple · 抖音 anpplex.
 */
object ContactConfig {
    private const val TAG = "StarLive"

    const val DEFAULT_AUTHOR_NAME = "大鹏 APEX"
    const val DEFAULT_AUTHOR_BIO = "教育人 · 科技数码 / 软件开发 / 硬件爱好者"
    const val DEFAULT_WECHAT = "anpple"
    const val DEFAULT_DOUYIN = "anpplex"
    const val DEFAULT_DOUYIN_HANDLE = "大鹏APEX"

    fun authorName(context: Context): String =
        readFirstLine(context, "contact/author_name.txt").ifBlank { DEFAULT_AUTHOR_NAME }

    fun authorBio(context: Context): String =
        readFirstLine(context, "contact/author_bio.txt").ifBlank { DEFAULT_AUTHOR_BIO }

    fun wechatId(context: Context): String {
        val v = readFirstLine(context, "contact/wechat.txt")
        return if (isPlaceholder(v)) DEFAULT_WECHAT else v
    }

    fun douyinId(context: Context): String {
        val v = readFirstLine(context, "contact/douyin.txt")
        return if (isPlaceholder(v)) DEFAULT_DOUYIN else v
    }

    fun douyinHandle(context: Context): String =
        readFirstLine(context, "contact/douyin_handle.txt").ifBlank { DEFAULT_DOUYIN_HANDLE }

    fun isPlaceholder(id: String): Boolean =
        id.isBlank() || id.contains("REPLACE_ME", ignoreCase = true)

    private fun readFirstLine(context: Context, assetPath: String): String {
        return runCatching {
            context.assets.open(assetPath).bufferedReader().use { r ->
                r.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .firstOrNull()
                    .orEmpty()
            }
        }.onFailure {
            // Optional override files may be absent.
            if (it !is java.io.FileNotFoundException) {
                Log.w(TAG, "contact load failed $assetPath", it)
            }
        }.getOrDefault("")
    }
}

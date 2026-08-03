package com.starlive.app.contact

import android.content.Context
import android.util.Log

object ContactConfig {
    private const val TAG = "StarLive"
    private const val ASSET = "contact/wechat.txt"

    fun wechatId(context: Context): String {
        return runCatching {
            context.assets.open(ASSET).bufferedReader().use { r ->
                r.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }
                    .firstOrNull()
                    .orEmpty()
            }
        }.onFailure { Log.w(TAG, "contact load failed", it) }
            .getOrDefault("")
            .ifBlank { "REPLACE_ME_WECHAT" }
    }

    fun isPlaceholder(id: String): Boolean =
        id.isBlank() || id.contains("REPLACE_ME", ignoreCase = true)
}

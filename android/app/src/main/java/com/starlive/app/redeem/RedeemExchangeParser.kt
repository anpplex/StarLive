package com.starlive.app.redeem

/**
 * Pure HTTP body → exchange result (no network, no Android JSONObject).
 * Unit-testable on JVM.
 */
object RedeemExchangeParser {
    data class Ok(
        val packId: String,
        val title: String,
        val version: Int,
        val sha256: String,
        val downloadUrl: String,
        val alreadyBound: Boolean,
    )

    /**
     * @throws IllegalStateException with user-facing Chinese message on failure
     */
    fun parse(httpCode: Int, body: String): Ok {
        val text = body.trim().ifBlank { "{}" }
        val ok = boolField(text, "ok") == true
        if (httpCode !in 200..299 || !ok) {
            val serverErr = stringField(text, "error")?.ifBlank { null }
            throw IllegalStateException(userMessage(httpCode, serverErr, null))
        }
        val packId = stringField(text, "pack_id").orEmpty()
        val downloadUrl = stringField(text, "download_url").orEmpty()
        if (packId.isBlank() || downloadUrl.isBlank()) {
            throw IllegalStateException("兑换成功但缺少 pack_id / download_url")
        }
        return Ok(
            packId = packId,
            title = stringField(text, "title")?.ifBlank { null } ?: packId,
            version = intField(text, "version") ?: 1,
            sha256 = stringField(text, "sha256").orEmpty(),
            downloadUrl = downloadUrl,
            alreadyBound = boolField(text, "already_bound") == true,
        )
    }

    fun userMessage(httpCode: Int, serverError: String?, fallback: String?): String {
        val s = serverError?.trim().orEmpty()
        if (s.isNotEmpty()) return s
        return when (httpCode) {
            0 -> "网络不可用 · 请检查车机联网后重试"
            in 200..299 -> fallback ?: "兑换失败"
            400 -> "兑换码无效或格式错误"
            401, 403 -> "兑换码不可用"
            404 -> "兑换服务不存在或地址错误"
            409 -> "该兑换码已达设备上限"
            410 -> "兑换码已作废或过期"
            429 -> "请求过于频繁 · 请稍后再试"
            in 500..599 -> "兑换服务暂时不可用 ($httpCode)"
            else -> fallback ?: "兑换失败 ($httpCode)"
        }
    }

    /** Minimal JSON field extractors (string / bool / int) for our API shape. */
    internal fun stringField(json: String, key: String): String? {
        val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        val m = re.find(json) ?: return null
        return m.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
    }

    internal fun boolField(json: String, key: String): Boolean? {
        val re = Regex(""""$key"\s*:\s*(true|false)""")
        val m = re.find(json) ?: return null
        return m.groupValues[1] == "true"
    }

    internal fun intField(json: String, key: String): Int? {
        val re = Regex(""""$key"\s*:\s*(-?\d+)""")
        val m = re.find(json) ?: return null
        return m.groupValues[1].toIntOrNull()
    }
}

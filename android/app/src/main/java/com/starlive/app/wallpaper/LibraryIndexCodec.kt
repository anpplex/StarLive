package com.starlive.app.wallpaper

/**
 * Pure encode/decode for library index.json (no Android JSONObject — JVM unit tests).
 * Format: [{"id":"...","label":"...","file":"...","kind":"static|gif|webp",
 *           "cropL":0,"cropT":0,"cropR":0,"cropB":0}, ...]
 * Optional fields: missing kind → infer from extension (else static); missing crop → 0.
 */
object LibraryIndexCodec {
    fun encode(items: List<WallpaperLibrary.Item>): String {
        return items.joinToString(prefix = "[", postfix = "]") { item ->
            val label = escape(item.label)
            val id = escape(item.id)
            val file = escape(item.fileName)
            val kind = escape(item.kind.ifBlank { AnimatedMedia.KIND_STATIC })
            val base =
                """{"id":"$id","label":"$label","file":"$file","kind":"$kind""""
            if (item.hasCrop()) {
                base +
                    ""","cropL":${fmt(item.cropL)},"cropT":${fmt(item.cropT)},""" +
                    """"cropR":${fmt(item.cropR)},"cropB":${fmt(item.cropB)}}"""
            } else {
                "$base}"
            }
        }
    }

    fun decode(raw: String): List<WallpaperLibrary.Item> {
        val text = raw.trim()
        if (text.isEmpty() || text == "[]") return emptyList()
        val out = mutableListOf<WallpaperLibrary.Item>()
        // Split objects roughly; fields extracted with same regex style as RedeemExchangeParser
        val objRe = Regex("""\{[^{}]*\}""")
        for (m in objRe.findAll(text)) {
            val o = m.value
            val id = stringField(o, "id") ?: continue
            val file = stringField(o, "file") ?: continue
            val label = stringField(o, "label") ?: id
            val kindRaw = stringField(o, "kind")
            val kind = when {
                !kindRaw.isNullOrBlank() -> kindRaw
                file.endsWith(".gif", ignoreCase = true) -> AnimatedMedia.KIND_GIF
                file.endsWith(".webp", ignoreCase = true) -> AnimatedMedia.KIND_WEBP
                else -> AnimatedMedia.KIND_STATIC
            }
            val cropL = numberField(o, "cropL") ?: 0f
            val cropT = numberField(o, "cropT") ?: 0f
            val cropR = numberField(o, "cropR") ?: 0f
            val cropB = numberField(o, "cropB") ?: 0f
            out += WallpaperLibrary.Item(
                id = id,
                label = label,
                fileName = file,
                kind = kind,
                cropL = cropL,
                cropT = cropT,
                cropR = cropR,
                cropB = cropB,
            )
        }
        return out
    }

    private fun stringField(json: String, key: String): String? {
        val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        val m = re.find(json) ?: return null
        return unescape(m.groupValues[1])
    }

    private fun numberField(json: String, key: String): Float? {
        val re = Regex(""""$key"\s*:\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)""")
        val m = re.find(json) ?: return null
        return m.groupValues[1].toFloatOrNull()
    }

    private fun fmt(v: Float): String {
        // Avoid scientific notation / trailing .0 noise for integers
        return if (v == v.toLong().toFloat()) v.toLong().toString() else v.toString()
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun unescape(s: String): String =
        s.replace("\\\"", "\"").replace("\\\\", "\\")
}

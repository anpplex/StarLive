package com.starlive.app.wallpaper

/**
 * Pure encode/decode for library index.json (no Android JSONObject — JVM unit tests).
 * Format: [{"id":"...","label":"...","file":"..."}, ...]
 */
object LibraryIndexCodec {
    fun encode(items: List<WallpaperLibrary.Item>): String {
        return items.joinToString(prefix = "[", postfix = "]") { item ->
            val label = escape(item.label)
            val id = escape(item.id)
            val file = escape(item.fileName)
            """{"id":"$id","label":"$label","file":"$file"}"""
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
            out += WallpaperLibrary.Item(id = id, label = label, fileName = file)
        }
        return out
    }

    private fun stringField(json: String, key: String): String? {
        val re = Regex(""""$key"\s*:\s*"((?:\\.|[^"\\])*)"""")
        val m = re.find(json) ?: return null
        return unescape(m.groupValues[1])
    }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun unescape(s: String): String =
        s.replace("\\\"", "\"").replace("\\\\", "\\")
}

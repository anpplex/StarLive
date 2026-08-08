package com.starlive.app.wallpaper

/**
 * Pure byte-sniff helpers for GIF / WebP (no Android framework dependency — JVM unit-testable).
 */
object AnimatedMedia {
    const val KIND_STATIC = "static"
    const val KIND_GIF = "gif"
    const val KIND_WEBP = "webp"

    fun isGif(bytes: ByteArray): Boolean {
        if (bytes.size < 6) return false
        return bytes[0] == 'G'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == '8'.code.toByte() &&
            (bytes[4] == '7'.code.toByte() || bytes[4] == '9'.code.toByte()) &&
            bytes[5] == 'a'.code.toByte()
    }

    fun isWebp(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        return bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() &&
            bytes[9] == 'E'.code.toByte() &&
            bytes[10] == 'B'.code.toByte() &&
            bytes[11] == 'P'.code.toByte()
    }

    /**
     * Animated WebP: VP8X chunk with animation flag (bit 1), or presence of ANIM chunk.
     */
    fun isAnimatedWebp(bytes: ByteArray): Boolean {
        if (!isWebp(bytes)) return false
        var i = 12
        while (i + 8 <= bytes.size) {
            val tag0 = bytes[i].toInt().toChar()
            val tag1 = bytes[i + 1].toInt().toChar()
            val tag2 = bytes[i + 2].toInt().toChar()
            val tag3 = bytes[i + 3].toInt().toChar()
            val tag = "$tag0$tag1$tag2$tag3"
            val size = (bytes[i + 4].toInt() and 0xff) or
                ((bytes[i + 5].toInt() and 0xff) shl 8) or
                ((bytes[i + 6].toInt() and 0xff) shl 16) or
                ((bytes[i + 7].toInt() and 0xff) shl 24)
            if (size < 0) break
            if (tag == "VP8X" && i + 8 < bytes.size) {
                val flags = bytes[i + 8].toInt() and 0xff
                return (flags and 0x02) != 0
            }
            if (tag == "ANIM") return true
            // Chunk payload is padded to even length
            val payload = size + (size and 1)
            val next = i + 8 + payload
            if (next <= i) break
            i = next
        }
        return false
    }

    /** Resolve kind from MIME string alone (`image/gif` → gif, `image/webp` → webp). */
    fun kindFromMime(mime: String?): String? {
        if (mime.isNullOrBlank()) return null
        val m = mime.trim().lowercase()
        return when {
            m == "image/gif" || m.endsWith("/gif") || m == "gif" -> KIND_GIF
            m == "image/webp" || m.endsWith("/webp") || m == "webp" -> KIND_WEBP
            m == "static" || m.startsWith("image/jpeg") || m.startsWith("image/png") ||
                m == "image/jpg" -> KIND_STATIC
            else -> null
        }
    }

    /** Sniff kind from magic bytes. Static WebP → [KIND_STATIC]; animated WebP → [KIND_WEBP]. */
    fun kindFromBytes(bytes: ByteArray): String = when {
        isGif(bytes) -> KIND_GIF
        isWebp(bytes) -> if (isAnimatedWebp(bytes)) KIND_WEBP else KIND_STATIC
        else -> KIND_STATIC
    }

    /**
     * Prefer explicit mime/kind token; fall back to bytes.
     * [mimeOrKind] may be a MIME (`image/gif`) or a kind (`gif`/`webp`/`static`).
     */
    fun kindFromMimeOrBytes(mimeOrKind: String?, bytes: ByteArray): String {
        val token = mimeOrKind?.trim()?.lowercase()
        when (token) {
            KIND_GIF, KIND_WEBP, KIND_STATIC -> return token
        }
        kindFromMime(mimeOrKind)?.let { fromMime ->
            // MIME image/webp may be static or animated — prefer bytes when present
            if (fromMime == KIND_WEBP && bytes.isNotEmpty()) {
                return if (isAnimatedWebp(bytes) || isWebp(bytes)) KIND_WEBP else KIND_STATIC
            }
            if (fromMime == KIND_GIF) return KIND_GIF
            if (fromMime == KIND_STATIC) return KIND_STATIC
        }
        return kindFromBytes(bytes)
    }

    fun sniffMime(bytes: ByteArray): String? = when {
        isGif(bytes) -> "image/gif"
        isWebp(bytes) -> "image/webp"
        bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte() -> "image/jpeg"
        bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() &&
            bytes[3] == 'G'.code.toByte() -> "image/png"
        else -> null
    }

    fun extensionForKind(kind: String): String = when (kind) {
        KIND_GIF -> "gif"
        KIND_WEBP -> "webp"
        else -> "jpg"
    }

    fun isAnimatedKind(kind: String): Boolean =
        kind == KIND_GIF || kind == KIND_WEBP
}

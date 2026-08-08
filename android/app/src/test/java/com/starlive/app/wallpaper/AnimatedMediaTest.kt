package com.starlive.app.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimatedMediaTest {
    @Test
    fun isGif_sniffs_header() {
        val gif87 = "GIF87a".toByteArray() + ByteArray(10)
        val gif89 = "GIF89a".toByteArray() + ByteArray(10)
        assertTrue(AnimatedMedia.isGif(gif87))
        assertTrue(AnimatedMedia.isGif(gif89))
        assertFalse(AnimatedMedia.isGif(byteArrayOf(0, 1, 2)))
        assertEquals(AnimatedMedia.KIND_GIF, AnimatedMedia.kindFromBytes(gif89))
        assertEquals("image/gif", AnimatedMedia.sniffMime(gif89))
    }

    @Test
    fun isWebp_sniffs_riff() {
        val webp = ByteArray(20)
        "RIFF".toByteArray().copyInto(webp, 0)
        "WEBP".toByteArray().copyInto(webp, 8)
        assertTrue(AnimatedMedia.isWebp(webp))
        assertEquals("image/webp", AnimatedMedia.sniffMime(webp))
    }

    @Test
    fun isAnimatedWebp_vp8x_flag() {
        // RIFF....WEBP + VP8X chunk with animation bit (0x02)
        val arr = buildBytes(
            "RIFF".toByteArray(),
            byteArrayOf(0, 0, 0, 0),
            "WEBP".toByteArray(),
            "VP8X".toByteArray(),
            byteArrayOf(10, 0, 0, 0), // chunk size little-endian
            byteArrayOf(0x02, 0, 0, 0, 0, 0, 0, 0, 0, 0), // flags + canvas
        )
        assertTrue(AnimatedMedia.isWebp(arr))
        assertTrue(AnimatedMedia.isAnimatedWebp(arr))
        assertEquals(AnimatedMedia.KIND_WEBP, AnimatedMedia.kindFromBytes(arr))
    }

    @Test
    fun static_webp_not_animated() {
        val arr = buildBytes(
            "RIFF".toByteArray(),
            byteArrayOf(0, 0, 0, 0),
            "WEBP".toByteArray(),
            "VP8 ".toByteArray(),
            byteArrayOf(4, 0, 0, 0),
            byteArrayOf(0, 0, 0, 0),
        )
        assertTrue(AnimatedMedia.isWebp(arr))
        assertFalse(AnimatedMedia.isAnimatedWebp(arr))
        assertEquals(AnimatedMedia.KIND_STATIC, AnimatedMedia.kindFromBytes(arr))
    }

    private fun buildBytes(vararg parts: ByteArray): ByteArray {
        val size = parts.sumOf { it.size }
        val out = ByteArray(size)
        var o = 0
        for (p in parts) {
            p.copyInto(out, o)
            o += p.size
        }
        return out
    }

    @Test
    fun kindFromMimeOrBytes() {
        assertEquals(AnimatedMedia.KIND_GIF, AnimatedMedia.kindFromMimeOrBytes("image/gif", ByteArray(0)))
        assertEquals(AnimatedMedia.KIND_GIF, AnimatedMedia.kindFromMimeOrBytes("gif", ByteArray(0)))
        assertEquals(AnimatedMedia.KIND_WEBP, AnimatedMedia.kindFromMimeOrBytes("webp", ByteArray(0)))
        assertEquals(AnimatedMedia.KIND_STATIC, AnimatedMedia.kindFromMimeOrBytes("image/jpeg", ByteArray(0)))
        val gif = "GIF89a".toByteArray() + ByteArray(8)
        assertEquals(AnimatedMedia.KIND_GIF, AnimatedMedia.kindFromMimeOrBytes(null, gif))
    }

    @Test
    fun extensionForKind() {
        assertEquals("gif", AnimatedMedia.extensionForKind(AnimatedMedia.KIND_GIF))
        assertEquals("webp", AnimatedMedia.extensionForKind(AnimatedMedia.KIND_WEBP))
        assertEquals("jpg", AnimatedMedia.extensionForKind(AnimatedMedia.KIND_STATIC))
    }
}

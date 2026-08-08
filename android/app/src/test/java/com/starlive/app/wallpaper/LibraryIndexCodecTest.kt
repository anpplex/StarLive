package com.starlive.app.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryIndexCodecTest {
    @Test
    fun round_trip() {
        val items = listOf(
            WallpaperLibrary.Item("lib_1", "夜色", "lib_1.jpg"),
            WallpaperLibrary.Item("lib_2", "quote \"x\"", "lib_2.jpg"),
        )
        val json = LibraryIndexCodec.encode(items)
        val back = LibraryIndexCodec.decode(json)
        assertEquals(2, back.size)
        assertEquals("lib_1", back[0].id)
        assertEquals("夜色", back[0].label)
        assertEquals("lib_1.jpg", back[0].fileName)
        assertEquals(AnimatedMedia.KIND_STATIC, back[0].kind)
        assertEquals("quote \"x\"", back[1].label)
        assertFalse(back[0].hasCrop())
    }

    @Test
    fun kind_and_crop_round_trip() {
        val items = listOf(
            WallpaperLibrary.Item(
                id = "lib_gif",
                label = "动图",
                fileName = "lib_gif.gif",
                kind = AnimatedMedia.KIND_GIF,
                cropL = 10f,
                cropT = 20f,
                cropR = 3100f,
                cropB = 304f,
            ),
            WallpaperLibrary.Item(
                id = "lib_webp",
                label = "webp anim",
                fileName = "lib_webp.webp",
                kind = AnimatedMedia.KIND_WEBP,
                cropL = 0.5f,
                cropT = 1.5f,
                cropR = 100.25f,
                cropB = 50.75f,
            ),
            WallpaperLibrary.Item(
                id = "lib_static",
                label = "still",
                fileName = "lib_static.jpg",
                kind = AnimatedMedia.KIND_STATIC,
            ),
        )
        val json = LibraryIndexCodec.encode(items)
        assertTrue(json.contains("\"kind\":\"gif\""))
        assertTrue(json.contains("\"kind\":\"webp\""))
        assertTrue(json.contains("\"cropL\":10"))
        val back = LibraryIndexCodec.decode(json)
        assertEquals(3, back.size)

        assertEquals(AnimatedMedia.KIND_GIF, back[0].kind)
        assertTrue(back[0].hasCrop())
        assertEquals(10f, back[0].cropL, 0.001f)
        assertEquals(20f, back[0].cropT, 0.001f)
        assertEquals(3100f, back[0].cropR, 0.001f)
        assertEquals(304f, back[0].cropB, 0.001f)

        assertEquals(AnimatedMedia.KIND_WEBP, back[1].kind)
        assertTrue(back[1].hasCrop())
        assertEquals(0.5f, back[1].cropL, 0.001f)
        assertEquals(1.5f, back[1].cropT, 0.001f)
        assertEquals(100.25f, back[1].cropR, 0.001f)
        assertEquals(50.75f, back[1].cropB, 0.001f)

        assertEquals(AnimatedMedia.KIND_STATIC, back[2].kind)
        assertFalse(back[2].hasCrop())
    }

    @Test
    fun legacy_decode_without_kind_or_crop() {
        val legacy =
            """[{"id":"lib_1","label":"夜色","file":"lib_1.jpg"},""" +
                """{"id":"lib_g","label":"g","file":"lib_g.gif"},""" +
                """{"id":"lib_w","label":"w","file":"lib_w.webp"}]"""
        val back = LibraryIndexCodec.decode(legacy)
        assertEquals(3, back.size)
        assertEquals(AnimatedMedia.KIND_STATIC, back[0].kind)
        assertEquals(0f, back[0].cropL, 0f)
        assertFalse(back[0].hasCrop())
        // Infer kind from extension when kind field missing
        assertEquals(AnimatedMedia.KIND_GIF, back[1].kind)
        assertEquals(AnimatedMedia.KIND_WEBP, back[2].kind)
    }

    @Test
    fun empty() {
        assertEquals(emptyList<WallpaperLibrary.Item>(), LibraryIndexCodec.decode("[]"))
        assertEquals(emptyList<WallpaperLibrary.Item>(), LibraryIndexCodec.decode(""))
    }

    @Test
    fun hasCrop_requires_positive_extent() {
        assertFalse(
            WallpaperLibrary.Item("a", "a", "a.jpg", cropL = 0f, cropT = 0f, cropR = 0f, cropB = 0f)
                .hasCrop(),
        )
        assertFalse(
            WallpaperLibrary.Item("a", "a", "a.jpg", cropL = 10f, cropT = 10f, cropR = 10f, cropB = 20f)
                .hasCrop(),
        )
        assertTrue(
            WallpaperLibrary.Item("a", "a", "a.jpg", cropL = 0f, cropT = 0f, cropR = 1f, cropB = 1f)
                .hasCrop(),
        )
    }
}

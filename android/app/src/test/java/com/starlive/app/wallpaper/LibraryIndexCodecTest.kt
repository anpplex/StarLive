package com.starlive.app.wallpaper

import org.junit.Assert.assertEquals
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
        assertEquals("quote \"x\"", back[1].label)
    }

    @Test
    fun empty() {
        assertEquals(emptyList<WallpaperLibrary.Item>(), LibraryIndexCodec.decode("[]"))
        assertEquals(emptyList<WallpaperLibrary.Item>(), LibraryIndexCodec.decode(""))
    }
}

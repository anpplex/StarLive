package com.starlive.app.runtime

import android.media.AudioAttributes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicPlaybackFilterTest {

    @Test
    fun wallpaper_engine_started_does_not_count() {
        val players = listOf(
            MusicPlaybackFilter.Player(
                packageName = "io.wallpaperengine.weclient",
                usage = AudioAttributes.USAGE_UNKNOWN,
                started = true,
            ),
        )
        assertFalse(
            MusicPlaybackFilter.isEffectivelyMusicPlaying(players, isMusicActiveFallback = true),
        )
    }

    @Test
    fun motif_car_wallpaper_ignored() {
        val players = listOf(
            MusicPlaybackFilter.Player(
                packageName = "com.motif.car",
                usage = AudioAttributes.USAGE_UNKNOWN,
                started = true,
            ),
        )
        assertFalse(MusicPlaybackFilter.isEffectivelyMusicPlaying(players, true))
    }

    @Test
    fun real_media_usage_counts() {
        val players = listOf(
            MusicPlaybackFilter.Player(
                packageName = "com.netease.cloudmusic.iot",
                usage = AudioAttributes.USAGE_MEDIA,
                started = true,
            ),
        )
        assertTrue(MusicPlaybackFilter.isEffectivelyMusicPlaying(players, false))
    }

    @Test
    fun unknown_music_app_counts() {
        val players = listOf(
            MusicPlaybackFilter.Player(
                packageName = "com.tencent.qqmusiccar",
                usage = AudioAttributes.USAGE_UNKNOWN,
                started = true,
            ),
        )
        assertTrue(MusicPlaybackFilter.isEffectivelyMusicPlaying(players, true))
    }

    @Test
    fun assistant_speech_ignored() {
        val players = listOf(
            MusicPlaybackFilter.Player(
                packageName = "com.huawei.vassistantcar",
                usage = AudioAttributes.USAGE_ASSISTANT,
                started = true,
            ),
        )
        assertFalse(MusicPlaybackFilter.isEffectivelyMusicPlaying(players, true))
    }

    @Test
    fun empty_players_uses_fallback() {
        assertTrue(MusicPlaybackFilter.isEffectivelyMusicPlaying(emptyList(), true))
        assertFalse(MusicPlaybackFilter.isEffectivelyMusicPlaying(emptyList(), false))
    }

    @Test
    fun unknown_without_package_does_not_count() {
        val players = listOf(
            MusicPlaybackFilter.Player(
                packageName = null,
                usage = AudioAttributes.USAGE_UNKNOWN,
                started = true,
            ),
        )
        // Reflection miss must not false-positive wallpaper as music.
        assertFalse(MusicPlaybackFilter.isEffectivelyMusicPlaying(players, true))
    }

    @Test
    fun configs_present_none_started_ignores_sticky_music_active() {
        val players = listOf(
            MusicPlaybackFilter.Player(
                packageName = "io.wallpaperengine.weclient",
                usage = AudioAttributes.USAGE_UNKNOWN,
                started = false,
            ),
        )
        assertFalse(MusicPlaybackFilter.isEffectivelyMusicPlaying(players, true))
    }

    @Test
    fun package_denylist_heuristic() {
        assertTrue(MusicPlaybackFilter.isIgnoredPackage("io.wallpaperengine.weclient"))
        assertTrue(MusicPlaybackFilter.isIgnoredPackage("com.foo.wallpaper.service"))
        assertFalse(MusicPlaybackFilter.isIgnoredPackage("com.netease.cloudmusic"))
    }
}

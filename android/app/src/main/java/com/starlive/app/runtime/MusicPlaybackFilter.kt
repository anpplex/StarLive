package com.starlive.app.runtime

import android.media.AudioAttributes

/**
 * Decide whether an active audio player should trigger **播歌让出**.
 *
 * Car HUs often run Wallpaper Engine / OEM live wallpapers with MediaPlayer
 * (USAGE_UNKNOWN). [AudioManager.isMusicActive] then stays true and star-ring
 * never applies. Filter by usage + package denylist (Lyra-adjacent policy:
 * yield only for real media apps).
 */
object MusicPlaybackFilter {

    data class Player(
        val packageName: String?,
        val usage: Int,
        val started: Boolean,
    )

    /**
     * @param players active playback rows (started or not); only [Player.started] count
     * @param isMusicActiveFallback [AudioManager.isMusicActive] when no configs
     */
    fun isEffectivelyMusicPlaying(
        players: List<Player>,
        isMusicActiveFallback: Boolean,
    ): Boolean {
        val started = players.filter { it.started }
        if (started.isEmpty()) {
            // Configs present but none started → not music (ignore sticky isMusicActive).
            if (players.isNotEmpty()) return false
            // No configs at all — last-resort legacy.
            return isMusicActiveFallback
        }
        return started.any { isMusicLike(it) }
    }

    fun isMusicLike(player: Player): Boolean {
        if (!player.started) return false
        val pkg = player.packageName?.trim().orEmpty()
        if (pkg.isNotEmpty() && isIgnoredPackage(pkg)) return false
        return when (player.usage) {
            AudioAttributes.USAGE_MEDIA,
            AudioAttributes.USAGE_GAME,
            -> true
            // Car music apps often use UNKNOWN. Only count when package is known
            // and not denylisted — null package (reflection miss) would false-positive
            // on Wallpaper Engine and pin yield forever.
            AudioAttributes.USAGE_UNKNOWN ->
                pkg.isNotEmpty() && !isIgnoredPackage(pkg)
            else -> false
        }
    }

    fun isIgnoredPackage(packageName: String): Boolean {
        val p = packageName.lowercase()
        if (p in DENY_EXACT) return true
        if (p.contains("wallpaper")) return true
        if (p.contains("livepaper")) return true
        if (p.endsWith(".weclient")) return true
        if (p.contains("wallpaperengine")) return true
        return false
    }

    /** Known HU wallpaper / ambient video players that keep isMusicActive sticky. */
    private val DENY_EXACT = setOf(
        "io.wallpaperengine.weclient",
        "com.motif.car",
        "com.motif.car.wallpaper",
    )
}

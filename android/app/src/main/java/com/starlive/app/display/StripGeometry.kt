package com.starlive.app.display

/**
 * Avatr star-ring geometry — must stay aligned with Lyra [EffectSurfaceProfile]
 * wallpaper band for handoff (see docs/LYRA-UPGRADE.md).
 */
object StripGeometry {
    const val STRIP_W = 4032
    const val STRIP_H = 284
    const val GAUGE_RESERVE = 1042
    const val WALLPAPER_W = 2990
    const val WALLPAPER_H = 284
    const val EDGE_FEATHER_DAY = 88
    const val EDGE_FEATHER_NIGHT = 104

    /** Glass plate under wallpaper band (day / night). */
    const val GLASS_DAY = 0xFFE8EAEE.toInt()
    const val GLASS_NIGHT = 0xFF080A0B.toInt()
}

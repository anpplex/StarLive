package com.starlive.app.display

/**
 * User-facing copy when cluster apply fails (C4 emulator / no strip). Pure, unit-testable.
 */
object ClusterApplyMessages {
    fun noCluster(probe: String): String =
        "星环屏不可达（模拟器/无 cluster 时常见）· $probe"

    fun launchFailed(): String = "无法上屏 · 请确认星环 Display 可用"

    fun isNoClusterHint(message: String): Boolean =
        message.contains("星环屏不可达") || message.contains("cluster", ignoreCase = true)
}

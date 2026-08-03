package com.starlive.app.display

/**
 * User-facing copy when cluster apply fails (C4 emulator / no strip). Pure, unit-testable.
 * Probe details stay in logs only — never surface raw display dumps to UI.
 */
object ClusterApplyMessages {
    fun noCluster(probe: String): String {
        // Keep probe param for call-site compatibility; do not put it in UI copy.
        @Suppress("UNUSED_PARAMETER")
        val ignored = probe
        return "当前设备未检测到星环屏"
    }

    fun launchFailed(): String = "无法显示到星环，请稍后重试"

    fun isNoClusterHint(message: String): Boolean =
        message.contains("未检测到星环屏") || message.contains("星环屏不可达")
}

package com.starlive.app.upgrade

import android.content.Context
import android.content.pm.PackageManager

object LyraPresence {
    /** Lyra product applicationId (Codex/Lyra). */
    const val LYRA_PACKAGE = "com.lyra.cluster"

    fun isInstalled(context: Context): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(LYRA_PACKAGE, 0)
            true
        }.getOrDefault(false)
    }

    fun launchLyra(context: Context): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(LYRA_PACKAGE) ?: return false
        launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launch)
            true
        }.getOrDefault(false)
    }
}

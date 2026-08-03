package com.starlive.app.device

import android.content.Context
import android.provider.Settings
import java.util.UUID

object DeviceIdentity {
    private const val PREFS = "starlive_device"
    private const val KEY = "device_id"

    fun deviceId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY, null)
        if (!existing.isNullOrBlank()) return existing
        val androidId = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ANDROID_ID,
        )
        val id = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            "starlive-$androidId"
        } else {
            "starlive-${UUID.randomUUID()}"
        }
        prefs.edit().putString(KEY, id).apply()
        return id
    }
}

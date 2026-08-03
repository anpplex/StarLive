package com.starlive.app.runtime

import android.content.Context

/** Persist "apply after play ends" intent (Phase 3 consumes; Phase 2 clears on restore). */
object PendingApplyStore {
    private const val PREFS = "starlive_wallpaper"
    private const val KEY = "pending_apply"

    fun isPending(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY, false)

    fun setPending(context: Context, value: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY, value)
            .apply()
    }

    fun clear(context: Context) = setPending(context, false)
}

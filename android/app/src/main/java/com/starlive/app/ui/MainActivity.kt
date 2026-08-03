package com.starlive.app.ui

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.BuildConfig
import com.starlive.app.R

/**
 * Home shell (Phase 0). Full cockpit UI lands in Phase 1 per INTERACTION-1.0.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0E12"))
            setPadding(dp(24), dp(20), dp(24), dp(20))
            gravity = Gravity.CENTER_VERTICAL
        }

        root.addView(
            TextView(this).apply {
                text = getString(R.string.app_name)
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            },
        )
        root.addView(
            TextView(this).apply {
                text = "StarLive · ${BuildConfig.VERSION_NAME}"
                setTextColor(Color.parseColor("#8B9BB4"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, dp(6), 0, dp(16))
            },
        )
        root.addView(
            TextView(this).apply {
                text = getString(R.string.skeleton_hint)
                setTextColor(Color.parseColor("#C5D0E0"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            },
        )

        setContentView(root)
    }
}

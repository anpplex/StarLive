package com.starlive.app.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.BuildConfig
import com.starlive.app.R
import com.starlive.app.StarLiveApp
import com.starlive.app.display.StripGeometry
import com.starlive.app.wallpaper.WallpaperRepository

/**
 * Home cockpit: preview + apply + import + demos + idle switch.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var preview: ImageView
    private lateinit var statusTv: TextView
    private lateinit var sourceTv: TextView
    private lateinit var heroSub: TextView

    private val orch get() = (application as StarLiveApp).orchestrator

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        startActivity(ImportConfirmActivity.intentFromUri(this, uri, "相册"))
    }

    private val requestReadImages = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            openDownloadImport()
        } else {
            Toast.makeText(this, R.string.need_storage, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WallpaperRepository.ensureSeeded(this)
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0E12"))
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }

        // Top bar
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(
            TextView(this).apply {
                text = getString(R.string.app_name)
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        statusTv = TextView(this).apply {
            setTextColor(Color.parseColor("#7DDEA0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setBackgroundColor(Color.parseColor("#1A2430"))
        }
        top.addView(statusTv)
        root.addView(top)

        root.addView(
            TextView(this).apply {
                text = "StarLive · ${BuildConfig.VERSION_NAME}"
                setTextColor(Color.parseColor("#6B7A90"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(4), 0, dp(10))
            },
        )

        // Hero preview: gauge + band
        val heroH = dp(56)
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heroH,
            )
            setBackgroundColor(Color.parseColor("#151A22"))
        }
        val gaugeW = (heroH * StripGeometry.GAUGE_RESERVE / StripGeometry.STRIP_H)
        hero.addView(
            TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(gaugeW, heroH)
                setBackgroundColor(Color.parseColor("#2A303A"))
                text = "表盘"
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#5A6578"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            },
        )
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = LinearLayout.LayoutParams(0, heroH, 1f)
            setOnClickListener { applyWallpaper() }
        }
        hero.addView(preview)
        root.addView(hero)

        heroSub = TextView(this).apply {
            setTextColor(Color.parseColor("#9AABB8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(8), 0, dp(12))
        }
        root.addView(heroSub)

        // CTAs
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        row.addView(primaryBtn("应用当前") { applyWallpaper() }.also {
            (it.layoutParams as LinearLayout.LayoutParams).apply {
                width = 0
                weight = 1f
                marginEnd = dp(8)
            }
        })
        row.addView(secondaryBtn("导入图片") { showImportSheet() }.also {
            (it.layoutParams as LinearLayout.LayoutParams).apply {
                width = 0
                weight = 1f
                marginEnd = dp(8)
            }
        })
        row.addView(secondaryBtn("我要定制") {
            Toast.makeText(this, "Phase 4：定制页", Toast.LENGTH_SHORT).show()
        }.also {
            (it.layoutParams as LinearLayout.LayoutParams).width = 0
            (it.layoutParams as LinearLayout.LayoutParams).weight = 1f
        })
        root.addView(row)

        // Demos
        root.addView(
            TextView(this).apply {
                text = "示范壁纸"
                setTextColor(Color.parseColor("#8B9BB4"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(16), 0, dp(6))
            },
        )
        val chipScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val demos = WallpaperRepository.demos(this)
        demos.forEach { d ->
            chips.addView(
                Button(this).apply {
                    text = d.label
                    isAllCaps = false
                    setOnClickListener {
                        WallpaperRepository.applyDemo(this@MainActivity, d.id)
                        refreshUi()
                        Toast.makeText(this@MainActivity, "已选 ${d.label} · 点应用同步到星环", Toast.LENGTH_SHORT).show()
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(40),
                    ).apply { marginEnd = dp(8) }
                },
            )
        }
        chipScroll.addView(chips)
        root.addView(chipScroll)

        val sourceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(8))
        }
        sourceTv = TextView(this).apply {
            setTextColor(Color.parseColor("#C5D0E0"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        sourceRow.addView(sourceTv)
        sourceRow.addView(
            Button(this).apply {
                text = "恢复示范"
                isAllCaps = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setBackgroundColor(Color.parseColor("#243040"))
                setTextColor(Color.parseColor("#D0D8E8"))
                setOnClickListener { confirmRestoreDemo() }
            },
        )
        root.addView(sourceRow)

        // Idle switch
        val idleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        idleRow.addView(
            TextView(this).apply {
                text = "空闲显示壁纸"
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        idleRow.addView(
            Switch(this).apply {
                isChecked = WallpaperRepository.idlePrefer(this@MainActivity)
                setOnCheckedChangeListener { _, checked ->
                    orch.setIdlePrefer(checked)
                    refreshUi()
                    Toast.makeText(
                        this@MainActivity,
                        if (checked) "空闲显示壁纸" else "已让出原厂星环",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )
        root.addView(idleRow)
        root.addView(
            TextView(this).apply {
                text = getString(R.string.idle_caption)
                setTextColor(Color.parseColor("#6B7A90"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(0, dp(4), 0, 0)
            },
        )

        if (!WallpaperRepository.firstRunHintShown(this)) {
            Toast.makeText(this, R.string.first_run_hint, Toast.LENGTH_LONG).show()
            WallpaperRepository.setFirstRunHintShown(this)
        }

        setContentView(root)
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun applyWallpaper() {
        if (orch.isEffectivelyPlaying()) {
            orch.applyCurrent("main-apply-deferred")
            refreshUi()
            Toast.makeText(this, R.string.toast_apply_deferred, Toast.LENGTH_SHORT).show()
            return
        }
        val ok = orch.applyCurrent("main-apply")
        refreshUi()
        Toast.makeText(
            this,
            if (ok) getString(R.string.toast_applied) else getString(R.string.toast_apply_fail),
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun showImportSheet() {
        val items = arrayOf(
            getString(R.string.import_from_picker),
            getString(R.string.import_from_download),
            getString(R.string.import_filename_help),
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.import_sheet_title)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> openPicker()
                    1 -> openDownloadImport()
                    2 -> showFilenameHelp()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openPicker() {
        runCatching {
            pickImage.launch("image/*")
        }.onFailure {
            Toast.makeText(this, R.string.picker_unavailable, Toast.LENGTH_LONG).show()
            openDownloadImport()
        }
    }

    private fun openDownloadImport() {
        val f = WallpaperRepository.findDownloadCandidate(this)
        if (f == null) {
            AlertDialog.Builder(this)
                .setTitle(R.string.no_download_title)
                .setMessage(R.string.no_download_msg)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.import_filename_help) { _, _ -> showFilenameHelp() }
                .show()
            return
        }
        startActivity(ImportConfirmActivity.intentFromPath(this, f.absolutePath, f.name))
    }

    private fun showFilenameHelp() {
        AlertDialog.Builder(this)
            .setTitle(R.string.import_filename_help)
            .setMessage(R.string.import_filename_detail)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun confirmRestoreDemo() {
        AlertDialog.Builder(this)
            .setTitle(R.string.restore_title)
            .setMessage(R.string.restore_msg)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.restore_demo) { _, _ ->
                WallpaperRepository.restoreDemo(this)
                refreshUi()
                Toast.makeText(this, R.string.restore_done, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun refreshUi() {
        sourceTv.text = "当前来源 · ${WallpaperRepository.labelForActive(this)}"
        val night = WallpaperRepository.isNightish(this)
        val f = WallpaperRepository.activeFile(this)
        if (f.isFile) {
            preview.setImageBitmap(BitmapFactory.decodeFile(f.absolutePath))
        }
        val idle = WallpaperRepository.idlePrefer(this)
        val showing = orch.showing && orch.display.isAlive()
        val playing = orch.isEffectivelyPlaying()
        when {
            playing && idle -> {
                statusTv.text = getString(R.string.status_yield_playing)
                statusTv.setTextColor(Color.parseColor("#8BB4E0"))
                heroSub.text = getString(R.string.hero_yield_playing)
            }
            !idle -> {
                statusTv.text = "已让出原厂"
                statusTv.setTextColor(Color.parseColor("#8B9BB4"))
                heroSub.text = "已让出原厂星环 · 开「空闲显示」后可再上屏"
            }
            showing -> {
                statusTv.text = "已上屏"
                statusTv.setTextColor(Color.parseColor("#7DDEA0"))
                heroSub.text = "星环与预览一致 · 点预览可再应用"
            }
            orch.lastError == "launch-failed" -> {
                statusTv.text = "无法上屏"
                statusTv.setTextColor(Color.parseColor("#E08A8A"))
                heroSub.text = "星环屏不可达（模拟器/无 cluster 时常见）· ${orch.display.listDisplaysForProbe()}"
            }
            else -> {
                statusTv.text = "未上屏"
                statusTv.setTextColor(Color.parseColor("#E0C48A"))
                heroSub.text = "已选「${WallpaperRepository.labelForActive(this)}」· 点应用看星环"
            }
        }
        // night unused except future dual — silence lint
        @Suppress("UNUSED_EXPRESSION")
        night
    }

    private fun primaryBtn(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setBackgroundColor(Color.parseColor("#3D6FE0"))
            setTextColor(Color.WHITE)
            setOnClickListener { onClick() }
        }

    private fun secondaryBtn(label: String, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setBackgroundColor(Color.parseColor("#243040"))
            setTextColor(Color.parseColor("#D0D8E8"))
            setOnClickListener { onClick() }
        }
}

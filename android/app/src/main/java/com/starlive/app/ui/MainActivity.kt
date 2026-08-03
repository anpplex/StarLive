package com.starlive.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.BuildConfig
import com.starlive.app.R
import com.starlive.app.StarLiveApp
import com.starlive.app.runtime.StripOrchestrator
import com.starlive.ring.StripGeometry
import com.starlive.app.wallpaper.WallpaperRepository

/**
 * Home cockpit: preview + apply + import + demos + idle switch.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var preview: ImageView
    private lateinit var statusTv: TextView
    private lateinit var sourceTv: TextView
    private lateinit var heroSub: TextView
    private lateinit var libraryHost: LinearLayout

    private val orch get() = (application as StarLiveApp).orchestrator

    private val uiRefreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == StripOrchestrator.ACTION_UI_REFRESH) {
                runOnUiThread {
                    if (::statusTv.isInitialized) refreshUi()
                }
            }
        }
    }

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
        top.addView(
            Button(this).apply {
                text = "关于"
                isAllCaps = false
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setBackgroundColor(Color.parseColor("#243040"))
                setTextColor(Color.WHITE)
                setOnClickListener {
                    startActivity(Intent(this@MainActivity, AboutActivity::class.java))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(36),
                ).apply { marginStart = dp(8) }
            },
        )
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
        fun rowLp(end: Int = 0) = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            if (end > 0) marginEnd = end
        }
        row.addView(primaryBtn("应用当前") { applyWallpaper() }, rowLp(dp(8)))
        row.addView(secondaryBtn("导入图片") { showImportSheet() }, rowLp(dp(8)))
        row.addView(
            secondaryBtn("我要定制") {
                startActivity(Intent(this, CustomActivity::class.java))
            },
            rowLp(),
        )
        root.addView(row)

        root.addView(
            secondaryBtn("兑换主题") {
                startActivity(Intent(this, RedeemActivity::class.java))
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )

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

        // Local library
        root.addView(
            TextView(this).apply {
                text = "已导入（长按删除）"
                setTextColor(Color.parseColor("#8B9BB4"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(12), 0, dp(6))
            },
        )
        libraryHost = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(libraryHost)
        rebuildLibraryRail()

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

        // Yield when Lyra installed
        val lyraRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        lyraRow.addView(
            TextView(this).apply {
                text = "已装 Lyra 时让路"
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        lyraRow.addView(
            Switch(this).apply {
                isChecked = WallpaperRepository.yieldWhenLyraInstalled(this@MainActivity)
                setOnCheckedChangeListener { _, checked ->
                    WallpaperRepository.setYieldWhenLyraInstalled(this@MainActivity, checked)
                    orch.refreshLyraHandoff("user-toggle")
                    refreshUi()
                    Toast.makeText(
                        this@MainActivity,
                        if (checked) "检测到 Lyra 时星澜不占星环" else "即使已装 Lyra 也可上壁纸",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        )
        root.addView(lyraRow)

        // Carousel
        val carRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        carRow.addView(
            TextView(this).apply {
                text = "示范轮播"
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        carRow.addView(
            Switch(this).apply {
                isChecked = WallpaperRepository.isCarouselEnabled(this@MainActivity)
                setOnCheckedChangeListener { _, checked ->
                    WallpaperRepository.setCarouselEnabled(this@MainActivity, checked)
                    (application as StarLiveApp).wallpaperCarousel.syncFromSettings()
                    refreshUi()
                }
            },
        )
        root.addView(carRow)
        val intervalRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        intervalRow.addView(
            TextView(this).apply {
                text = "轮播间隔"
                setTextColor(Color.parseColor("#C5D0E0"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        val intervalVal = TextView(this).apply {
            text = "${WallpaperRepository.carouselIntervalMinutes(this@MainActivity)} 分钟"
            setTextColor(Color.parseColor("#8BB4E0"))
            setPadding(dp(8), 0, dp(8), 0)
        }
        intervalRow.addView(
            Button(this).apply {
                text = "−"
                setOnClickListener {
                    val m = (WallpaperRepository.carouselIntervalMinutes(this@MainActivity) - 1)
                        .coerceAtLeast(WallpaperRepository.CAROUSEL_MIN)
                    WallpaperRepository.setCarouselIntervalMinutes(this@MainActivity, m)
                    intervalVal.text = "$m 分钟"
                    (application as StarLiveApp).wallpaperCarousel.reschedule()
                }
            },
        )
        intervalRow.addView(intervalVal)
        intervalRow.addView(
            Button(this).apply {
                text = "+"
                setOnClickListener {
                    val m = (WallpaperRepository.carouselIntervalMinutes(this@MainActivity) + 1)
                        .coerceAtMost(WallpaperRepository.CAROUSEL_MAX)
                    WallpaperRepository.setCarouselIntervalMinutes(this@MainActivity, m)
                    intervalVal.text = "$m 分钟"
                    (application as StarLiveApp).wallpaperCarousel.reschedule()
                }
            },
        )
        root.addView(intervalRow)

        // Night mode
        root.addView(
            TextView(this).apply {
                text = "日夜模式"
                setTextColor(Color.parseColor("#8B9BB4"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(0, dp(14), 0, dp(6))
            },
        )
        val nightRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("auto" to "自动", "dark" to "深", "light" to "浅").forEach { (mode, label) ->
            nightRow.addView(
                Button(this).apply {
                    text = label
                    isAllCaps = false
                    setOnClickListener {
                        WallpaperRepository.setNightMode(this@MainActivity, mode)
                        if (orch.showing) orch.applyCurrent("night-$mode")
                        refreshUi()
                    }
                    layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                        marginEnd = dp(6)
                    }
                    setBackgroundColor(Color.parseColor("#243040"))
                    setTextColor(Color.WHITE)
                },
            )
        }
        root.addView(nightRow)

        // Footer
        root.addView(
            TextView(this).apply {
                text = "规格说明  ·  升级到 Lyra 歌词特效"
                setTextColor(Color.parseColor("#5B8DEF"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(18), 0, dp(4))
                setOnClickListener {
                    // open chooser-like: two options
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setItems(arrayOf("规格说明", "升级到 Lyra")) { _, which ->
                            when (which) {
                                0 -> startActivity(Intent(this@MainActivity, SpecActivity::class.java))
                                1 -> startActivity(Intent(this@MainActivity, UpgradeActivity::class.java))
                            }
                        }
                        .show()
                }
            },
        )

        if (!WallpaperRepository.firstRunHintShown(this)) {
            Toast.makeText(this, R.string.first_run_hint, Toast.LENGTH_LONG).show()
            WallpaperRepository.setFirstRunHintShown(this)
        }

        // Car HU may be short landscape — keep all settings reachable
        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(Color.parseColor("#0B0E12"))
                isFillViewport = true
                addView(
                    root,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
            },
        )
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(StripOrchestrator.ACTION_UI_REFRESH)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(uiRefreshReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(uiRefreshReceiver, filter)
        }
        orch.refreshLyraHandoff("main-resume")
        // Some HUs block FGS from Application; re-assert KeepAlive when UI is visible
        if (WallpaperRepository.idlePrefer(this) && !orch.isHandedOffToLyra()) {
            runCatching {
                com.starlive.app.service.KeepAliveService.start(this)
            }
        }
        rebuildLibraryRail()
        refreshUi()
        maybeSoftHints()
        // process-start recover first tick ~0.4s — refresh capsule after apply
        statusTv.postDelayed({ if (!isFinishing) refreshUi() }, 900L)
        statusTv.postDelayed({ if (!isFinishing) refreshUi() }, 3_200L)
    }

    override fun onPause() {
        runCatching { unregisterReceiver(uiRefreshReceiver) }
        super.onPause()
    }

    private fun rebuildLibraryRail() {
        if (!::libraryHost.isInitialized) return
        libraryHost.removeAllViews()
        val dens = resources.displayMetrics.density
        fun dp(v: Int) = (v * dens).toInt()
        val items = WallpaperRepository.libraryItems(this)
        if (items.isEmpty()) {
            libraryHost.addView(
                TextView(this).apply {
                    text = "导入后会出现在这里，最多 24 张"
                    setTextColor(Color.parseColor("#5A6578"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                },
            )
            return
        }
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        items.forEach { item ->
            row.addView(
                Button(this).apply {
                    text = item.label.take(10)
                    isAllCaps = false
                    setBackgroundColor(Color.parseColor("#1E2A3A"))
                    setTextColor(Color.parseColor("#D0D8E8"))
                    setOnClickListener {
                        if (WallpaperRepository.applyLibraryItem(this@MainActivity, item.id)) {
                            refreshUi()
                            Toast.makeText(
                                this@MainActivity,
                                "已选 ${item.label} · 点应用同步到星环",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                    setOnLongClickListener {
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("删除导入？")
                            .setMessage(item.label)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton("删除") { _, _ ->
                                WallpaperRepository.deleteLibraryItem(this@MainActivity, item.id)
                                rebuildLibraryRail()
                                refreshUi()
                            }
                            .show()
                        true
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        dp(40),
                    ).apply { marginEnd = dp(8) }
                },
            )
        }
        scroll.addView(row)
        libraryHost.addView(scroll)
    }

    private fun maybeSoftHints() {
        if (!WallpaperRepository.nlsHintShown(this)) {
            WallpaperRepository.setNlsHintShown(this)
            // Non-blocking: only toast once
            Toast.makeText(this, R.string.nls_hint, Toast.LENGTH_LONG).show()
        }
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
        // MediaStore path needs images permission on some HUs
        val need = if (android.os.Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, need)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestReadImages.launch(need)
            return
        }
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
        orch.syncShowingFromDisplay()
        // Prefer live display probe: process-start recover launches cluster after onResume
        val alive = orch.display.isAlive()
        val launching = orch.showing && !alive
        val playing = orch.isEffectivelyPlaying()
        val handoff = orch.isHandedOffToLyra()
        when {
            handoff -> {
                statusTv.text = getString(R.string.status_handed_off)
                statusTv.setTextColor(Color.parseColor("#B8A0E0"))
                heroSub.text = getString(R.string.hero_handed_off)
            }
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
            alive -> {
                statusTv.text = "已上屏"
                statusTv.setTextColor(Color.parseColor("#7DDEA0"))
                heroSub.text = "星环与预览一致 · 点预览可再应用"
            }
            launching -> {
                statusTv.text = "上屏中…"
                statusTv.setTextColor(Color.parseColor("#8BB4E0"))
                heroSub.text = "正在打开星环 · 片刻后自动同步状态"
            }
            orch.lastError == "launch-failed" -> {
                statusTv.text = "无法上屏"
                statusTv.setTextColor(Color.parseColor("#E08A8A"))
                heroSub.text = com.starlive.app.display.ClusterApplyMessages.noCluster(
                    orch.display.listDisplaysForProbe(),
                )
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

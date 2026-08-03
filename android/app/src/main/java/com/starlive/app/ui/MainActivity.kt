package com.starlive.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.graphics.Typeface
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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.BuildConfig
import com.starlive.app.R
import com.starlive.app.StarLiveApp
import com.starlive.app.display.ClusterApplyMessages
import com.starlive.app.runtime.StripOrchestrator
import com.starlive.app.service.KeepAliveService
import com.starlive.app.ui.UiTokens.applyRoundedBg
import com.starlive.app.ui.UiTokens.dp
import com.starlive.app.wallpaper.WallpaperRepository
import com.starlive.ring.StripGeometry

/**
 * Home cockpit: preview + apply + import + demos + settings.
 * Layout tuned for car landscape (clear hierarchy, large hit targets).
 */
class MainActivity : AppCompatActivity() {
    private lateinit var preview: ImageView
    private lateinit var statusTv: TextView
    private lateinit var sourceTv: TextView
    private lateinit var heroSub: TextView
    private lateinit var libraryHost: LinearLayout
    private lateinit var demoHost: LinearLayout
    private var selectedDemoId: String? = null

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
        selectedDemoId = WallpaperRepository.activeId(this).takeIf { !it.startsWith("lib:") && it != "custom" }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiTokens.bg)
            setPadding(dp(20), dp(14), dp(20), dp(20))
        }

        root.addView(buildTopBar())
        root.addView(buildHeroCard())
        root.addView(buildActionRow())
        root.addView(
            UiKit.secondaryButton(this, "兑换主题") {
                startActivity(Intent(this, RedeemActivity::class.java))
            }.also {
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { topMargin = dp(8) }
            },
        )

        root.addView(UiKit.sectionTitle(this, "选择壁纸"))
        root.addView(buildLibraryCard())

        root.addView(UiKit.sectionTitle(this, "显示与恢复"))
        root.addView(buildSettingsCard())

        root.addView(UiKit.sectionTitle(this, "日夜"))
        root.addView(buildNightRow())

        root.addView(buildFooterLinks())

        if (!WallpaperRepository.firstRunHintShown(this)) {
            Toast.makeText(this, R.string.first_run_hint, Toast.LENGTH_LONG).show()
            WallpaperRepository.setFirstRunHintShown(this)
        }

        setContentView(
            ScrollView(this).apply {
                setBackgroundColor(UiTokens.bg)
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

    private fun buildTopBar(): LinearLayout {
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titles = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titles.addView(UiKit.title(this, getString(R.string.app_name)))
        titles.addView(
            TextView(this).apply {
                text = "StarLive · ${BuildConfig.VERSION_NAME}"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(0, dp(2), 0, 0)
            },
        )
        top.addView(titles)
        statusTv = UiKit.statusPill(this)
        top.addView(statusTv)
        top.addView(
            UiKit.ghostButton(this, "关于") {
                startActivity(Intent(this, AboutActivity::class.java))
            }.also {
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { marginStart = dp(8) }
            },
        )
        return top
    }

    private fun buildHeroCard(): LinearLayout {
        val card = UiKit.card(this)
        val heroH = dp(64)
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heroH,
            )
            applyRoundedBg(UiTokens.heroBg, 10f)
        }
        val gaugeW = (heroH * StripGeometry.GAUGE_RESERVE / StripGeometry.STRIP_H)
        hero.addView(
            TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(gaugeW, heroH)
                applyRoundedBg(UiTokens.gaugeBg, 0f)
                text = "表盘"
                gravity = Gravity.CENTER
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            },
        )
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = LinearLayout.LayoutParams(0, heroH, 1f)
            contentDescription = "星环预览，点按应用"
            setOnClickListener { applyWallpaper() }
        }
        hero.addView(preview)
        card.addView(hero)

        heroSub = TextView(this).apply {
            setTextColor(UiTokens.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(10), 0, dp(2))
            setLineSpacing(0f, 1.2f)
        }
        card.addView(heroSub)

        sourceTv = TextView(this).apply {
            setTextColor(UiTokens.textMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(2), 0, 0)
        }
        card.addView(sourceTv)
        return card
    }

    private fun buildActionRow(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(12) }
        }
        row.addView(UiKit.primaryButton(this, "应用当前") { applyWallpaper() }, UiKit.hGap(this, 1.2f, dp(8)))
        row.addView(UiKit.secondaryButton(this, "导入") { showImportSheet() }, UiKit.hGap(this, 1f, dp(8)))
        row.addView(
            UiKit.secondaryButton(this, "定制") {
                startActivity(Intent(this, CustomActivity::class.java))
            },
            UiKit.hGap(this, 1f),
        )
        return row
    }

    private fun buildLibraryCard(): LinearLayout {
        val card = UiKit.card(this)
        card.addView(
            TextView(this).apply {
                text = "示范"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            },
        )
        demoHost = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, dp(4))
        }
        val demoScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(demoHost)
        }
        card.addView(demoScroll)
        rebuildDemoChips()

        card.addView(
            TextView(this).apply {
                text = "已导入 · 点选应用预览 · 长按删除"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setPadding(0, dp(10), 0, 0)
            },
        )
        libraryHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(6), 0, 0)
        }
        card.addView(libraryHost)
        rebuildLibraryRail()

        val sourceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        sourceRow.addView(
            TextView(this).apply {
                text = "可随时恢复内置示范"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        sourceRow.addView(UiKit.ghostButton(this, "恢复示范") { confirmRestoreDemo() })
        card.addView(sourceRow)
        return card
    }

    private fun buildSettingsCard(): LinearLayout {
        val card = UiKit.card(this)
        card.addView(
            UiKit.settingRow(
                this,
                title = "空闲显示壁纸",
                subtitle = getString(R.string.idle_caption),
                initial = WallpaperRepository.idlePrefer(this),
            ) { checked ->
                orch.setIdlePrefer(checked)
                refreshUi()
                Toast.makeText(
                    this,
                    if (checked) "已开启空闲显示" else "已让出原厂星环",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
        card.addView(UiKit.spacer(this, 6))
        card.addView(
            UiKit.settingRow(
                this,
                title = "已装 Lyra 时让路",
                subtitle = "检测到 Lyra 时星澜不占星环，避免双开抢屏",
                initial = WallpaperRepository.yieldWhenLyraInstalled(this),
            ) { checked ->
                WallpaperRepository.setYieldWhenLyraInstalled(this, checked)
                orch.refreshLyraHandoff("user-toggle")
                refreshUi()
                Toast.makeText(
                    this,
                    if (checked) "检测到 Lyra 时星澜不占星环" else "即使已装 Lyra 也可上壁纸",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
        card.addView(UiKit.spacer(this, 6))
        card.addView(
            UiKit.settingRow(
                this,
                title = "示范轮播",
                subtitle = "仅本地示范图轮换（导入图不参与）",
                initial = WallpaperRepository.isCarouselEnabled(this),
            ) { checked ->
                WallpaperRepository.setCarouselEnabled(this, checked)
                (application as StarLiveApp).wallpaperCarousel.syncFromSettings()
                refreshUi()
            },
        )

        val intervalRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        intervalRow.addView(
            TextView(this).apply {
                text = "轮播间隔"
                setTextColor(UiTokens.textSecondary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        val intervalVal = TextView(this).apply {
            text = "${WallpaperRepository.carouselIntervalMinutes(this@MainActivity)} 分钟"
            setTextColor(UiTokens.info)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(dp(10), 0, dp(10), 0)
        }
        intervalRow.addView(
            UiKit.secondaryButton(this, "−") {
                val m = (WallpaperRepository.carouselIntervalMinutes(this) - 1)
                    .coerceAtLeast(WallpaperRepository.CAROUSEL_MIN)
                WallpaperRepository.setCarouselIntervalMinutes(this, m)
                intervalVal.text = "$m 分钟"
                (application as StarLiveApp).wallpaperCarousel.reschedule()
            }.also {
                it.layoutParams = LinearLayout.LayoutParams(dp(52), LinearLayout.LayoutParams.WRAP_CONTENT)
            },
        )
        intervalRow.addView(intervalVal)
        intervalRow.addView(
            UiKit.secondaryButton(this, "+") {
                val m = (WallpaperRepository.carouselIntervalMinutes(this) + 1)
                    .coerceAtMost(WallpaperRepository.CAROUSEL_MAX)
                WallpaperRepository.setCarouselIntervalMinutes(this, m)
                intervalVal.text = "$m 分钟"
                (application as StarLiveApp).wallpaperCarousel.reschedule()
            }.also {
                it.layoutParams = LinearLayout.LayoutParams(dp(52), LinearLayout.LayoutParams.WRAP_CONTENT)
            },
        )
        card.addView(intervalRow)
        return card
    }

    private fun buildNightRow(): LinearLayout {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val current = WallpaperRepository.nightMode(this)
        listOf("auto" to "自动", "dark" to "深色", "light" to "浅色").forEachIndexed { i, (mode, label) ->
            row.addView(
                UiKit.chip(this, label, selected = current == mode) {
                    WallpaperRepository.setNightMode(this, mode)
                    if (orch.showing || orch.display.isAlive()) orch.applyCurrent("night-$mode")
                    // rebuild chips selection by re-creating row parent is hard; refresh full night section via recreate chips:
                    (row.parent as? LinearLayout)?.let { parent ->
                        val idx = parent.indexOfChild(row)
                        parent.removeViewAt(idx)
                        parent.addView(buildNightRow(), idx)
                    }
                    refreshUi()
                }.also {
                    it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (i < 2) marginEnd = dp(8)
                    }
                },
            )
        }
        return row
    }

    private fun buildFooterLinks(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(16), 0, dp(4))
        }
        row.addView(
            UiKit.ghostButton(this, "规格说明") {
                startActivity(Intent(this, SpecActivity::class.java))
            },
        )
        row.addView(UiKit.spacer(this, 0).also {
            it.layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        })
        row.addView(
            UiKit.ghostButton(this, "升级到 Lyra") {
                startActivity(Intent(this, UpgradeActivity::class.java))
            },
        )
        return row
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
        if (WallpaperRepository.idlePrefer(this) && !orch.isHandedOffToLyra()) {
            runCatching { KeepAliveService.start(this) }
        }
        rebuildLibraryRail()
        rebuildDemoChips()
        refreshUi()
        maybeSoftHints()
        statusTv.postDelayed({ if (!isFinishing) refreshUi() }, 900L)
        statusTv.postDelayed({ if (!isFinishing) refreshUi() }, 3_200L)
    }

    override fun onPause() {
        runCatching { unregisterReceiver(uiRefreshReceiver) }
        super.onPause()
    }

    private fun rebuildDemoChips() {
        if (!::demoHost.isInitialized) return
        demoHost.removeAllViews()
        val active = WallpaperRepository.activeId(this)
        WallpaperRepository.demos(this).forEach { d ->
            val selected = active == d.id || selectedDemoId == d.id
            demoHost.addView(
                UiKit.chip(this, d.label, selected = selected) {
                    selectedDemoId = d.id
                    WallpaperRepository.applyDemo(this, d.id)
                    rebuildDemoChips()
                    refreshUi()
                    Toast.makeText(this, "已选 ${d.label} · 点「应用当前」同步到星环", Toast.LENGTH_SHORT).show()
                },
            )
        }
    }

    private fun rebuildLibraryRail() {
        if (!::libraryHost.isInitialized) return
        libraryHost.removeAllViews()
        val items = WallpaperRepository.libraryItems(this)
        if (items.isEmpty()) {
            libraryHost.addView(
                TextView(this).apply {
                    text = "还没有导入图 · 用「导入」或「兑换主题」添加，最多 24 张"
                    setTextColor(UiTokens.textMuted)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setPadding(0, dp(4), 0, dp(4))
                },
            )
            return
        }
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val active = WallpaperRepository.activeId(this)
        items.forEach { item ->
            val selected = active == "lib:${item.id}" || active == item.id
            row.addView(
                UiKit.chip(this, item.label.take(10), selected = selected) {
                    if (WallpaperRepository.applyLibraryItem(this, item.id)) {
                        selectedDemoId = null
                        rebuildDemoChips()
                        rebuildLibraryRail()
                        refreshUi()
                        Toast.makeText(this, "已选 ${item.label} · 点「应用当前」同步", Toast.LENGTH_SHORT).show()
                    }
                }.also { chip ->
                    chip.setOnLongClickListener {
                        AlertDialog.Builder(this)
                            .setTitle("删除导入？")
                            .setMessage(item.label)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton("删除") { _, _ ->
                                WallpaperRepository.deleteLibraryItem(this, item.id)
                                rebuildLibraryRail()
                                refreshUi()
                            }
                            .show()
                        true
                    }
                },
            )
        }
        scroll.addView(row)
        libraryHost.addView(scroll)
    }

    private fun maybeSoftHints() {
        if (!WallpaperRepository.nlsHintShown(this)) {
            WallpaperRepository.setNlsHintShown(this)
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
        val need = if (Build.VERSION.SDK_INT >= 33) {
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
                selectedDemoId = WallpaperRepository.activeId(this)
                rebuildDemoChips()
                rebuildLibraryRail()
                refreshUi()
                Toast.makeText(this, R.string.restore_done, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun refreshUi() {
        if (!::sourceTv.isInitialized) return
        sourceTv.text = "当前来源 · ${WallpaperRepository.labelForActive(this)}"
        val f = WallpaperRepository.activeFile(this)
        if (f.isFile && ::preview.isInitialized) {
            preview.setImageBitmap(BitmapFactory.decodeFile(f.absolutePath))
        }
        val idle = WallpaperRepository.idlePrefer(this)
        orch.syncShowingFromDisplay()
        val alive = orch.display.isAlive()
        val launching = orch.showing && !alive
        val playing = orch.isEffectivelyPlaying()
        val handoff = orch.isHandedOffToLyra()
        when {
            handoff -> styleStatus(
                getString(R.string.status_handed_off),
                UiTokens.lyra,
                getString(R.string.hero_handed_off),
            )
            playing && idle -> styleStatus(
                getString(R.string.status_yield_playing),
                UiTokens.info,
                getString(R.string.hero_yield_playing),
            )
            !idle -> styleStatus(
                "已让出原厂",
                UiTokens.textMuted,
                "已让出原厂星环 · 开「空闲显示」后可再上屏",
            )
            alive -> styleStatus(
                "已上屏",
                UiTokens.success,
                "星环与预览一致 · 点预览或「应用当前」可再同步",
            )
            launching -> styleStatus(
                "上屏中…",
                UiTokens.info,
                "正在打开星环 · 片刻后自动同步状态",
            )
            orch.lastError == "launch-failed" -> styleStatus(
                "无法上屏",
                UiTokens.danger,
                ClusterApplyMessages.noCluster(orch.display.listDisplaysForProbe()),
            )
            else -> styleStatus(
                "未上屏",
                UiTokens.warning,
                "已选「${WallpaperRepository.labelForActive(this)}」· 点「应用当前」同步到星环",
            )
        }
    }

    private fun styleStatus(label: String, color: Int, sub: String) {
        statusTv.text = label
        statusTv.setTextColor(color)
        heroSub.text = sub
    }
}

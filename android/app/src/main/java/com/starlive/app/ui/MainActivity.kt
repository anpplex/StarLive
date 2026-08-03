package com.starlive.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
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
import com.starlive.app.wallpaper.WallpaperLibrary
import com.starlive.app.wallpaper.WallpaperRepository
import com.starlive.ring.StripGeometry
import java.io.File

/**
 * Home cockpit (HMI-style): hero preview → pick wallpaper → one primary apply.
 * Low-frequency actions live under 「更多」.
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

    private var nightRowHost: LinearLayout? = null

    private val uiRefreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == StripOrchestrator.ACTION_UI_REFRESH) {
                runOnUiThread {
                    if (!::statusTv.isInitialized) return@runOnUiThread
                    val reason = intent.getStringExtra(StripOrchestrator.EXTRA_REASON).orEmpty()
                    if (reason.startsWith("ambient")) {
                        rebuildNightRow()
                        rebuildDemoChips()
                    }
                    refreshUi()
                }
            }
        }
    }

    /** SAF document picker — more reliable on multi-user car HUs than plain GetContent. */
    private val pickDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        startActivity(ImportConfirmActivity.intentFromUri(this, uri, "文件"))
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
            setPadding(dp(24), dp(16), dp(24), dp(24))
        }

        // 1) Header  2) Hero  3) 选择壁纸（最高频）  4) 唯一主 CTA
        root.addView(buildTopBar())
        root.addView(buildHeroCard())
        root.addView(buildPickHeader())
        root.addView(buildLibraryCard())
        root.addView(buildPrimaryApply())

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
            setPadding(0, 0, 0, dp(4))
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
            UiKit.ghostButton(this, "更多") { showMoreMenu() }.also {
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { marginStart = dp(10) }
            },
        )
        return top
    }

    private fun buildHeroCard(): LinearLayout {
        val card = UiKit.card(this)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(10) }
        // Taller strip preview — primary visual anchor (HMI-style)
        val heroH = dp(88)
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                heroH,
            )
            applyRoundedBg(UiTokens.heroBg, 14f, UiTokens.stroke)
        }
        val gaugeW = (heroH * StripGeometry.GAUGE_RESERVE / StripGeometry.STRIP_H).coerceAtLeast(dp(28))
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
            contentDescription = "星环预览，点按应用上屏"
            setOnClickListener { applyWallpaper() }
        }
        hero.addView(preview)
        card.addView(hero)

        heroSub = TextView(this).apply {
            setTextColor(UiTokens.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setPadding(0, dp(12), 0, dp(2))
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

    /** Section title row: 选择壁纸 + 导入 */
    private fun buildPickHeader(): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, dp(8))
        }
        row.addView(
            TextView(this).apply {
                text = "选择壁纸"
                setTextColor(UiTokens.textPrimary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        row.addView(
            UiKit.secondaryButton(this, "导入") { showImportSheet() }.also {
                it.minHeight = dp(40)
                it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            },
        )
        return row
    }

    private fun buildPrimaryApply(): Button =
        UiKit.primaryButton(this, "应用上屏") { applyWallpaper() }.also {
            it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(16) }
            it.minHeight = dp(52)
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
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
            setPadding(0, dp(8), 0, dp(4))
        }
        card.addView(
            HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(demoHost)
            },
        )
        rebuildDemoChips()

        card.addView(
            TextView(this).apply {
                text = "我的库 · 点选预览 · 长按删除"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setPadding(0, dp(14), 0, 0)
            },
        )
        libraryHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        card.addView(libraryHost)
        rebuildLibraryRail()

        val sourceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, 0)
        }
        sourceRow.addView(
            TextView(this).apply {
                text = "恢复内置示范"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        sourceRow.addView(UiKit.ghostButton(this, "恢复示范") { confirmRestoreDemo() })
        card.addView(sourceRow)
        return card
    }

    /** Vertical thumb + label chip for content rail (HMI media style). */
    private fun thumbChip(
        label: String,
        selected: Boolean,
        thumb: Bitmap?,
        onClick: () -> Unit,
        onLongClick: (() -> Boolean)? = null,
    ): LinearLayout {
        val w = dp(112)
        val h = dp(48)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { marginEnd = dp(10) }
            applyRoundedBg(
                if (selected) UiTokens.surface3 else UiTokens.surface2,
                14f,
                if (selected) UiTokens.accent else UiTokens.stroke,
            )
            setPadding(dp(6), dp(6), dp(6), dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            if (onLongClick != null) {
                setOnLongClickListener { onLongClick() }
            }
        }
        col.addView(
            ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(w, h)
                scaleType = ImageView.ScaleType.CENTER_CROP
                applyRoundedBg(UiTokens.heroBg, 8f)
                if (thumb != null) setImageBitmap(thumb)
                clipToOutline = true
            },
        )
        col.addView(
            TextView(this).apply {
                text = label
                gravity = Gravity.CENTER
                setTextColor(if (selected) UiTokens.textPrimary else UiTokens.textSecondary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = if (selected) {
                    Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                } else {
                    Typeface.DEFAULT
                }
                setPadding(0, dp(6), 0, 0)
                maxLines = 1
            },
        )
        return col
    }

    private fun loadDemoThumb(demo: WallpaperRepository.Demo): Bitmap? {
        return runCatching {
            val asset = demo.assetFor(WallpaperRepository.isNightish(this))
            assets.open(asset).use { input ->
                val raw = BitmapFactory.decodeStream(input) ?: return@runCatching null
                Bitmap.createScaledBitmap(raw, 224, 48, true).also {
                    if (it !== raw) raw.recycle()
                }
            }
        }.getOrNull()
    }

    private fun loadLibraryThumb(item: WallpaperLibrary.Item): Bitmap? {
        return runCatching {
            val f = item.file(this)
            if (!f.isFile) return@runCatching null
            val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
            val raw = BitmapFactory.decodeFile(f.absolutePath, opts) ?: return@runCatching null
            Bitmap.createScaledBitmap(raw, 224, 48, true).also {
                if (it !== raw) raw.recycle()
            }
        }.getOrNull()
    }

    /** Human-readable library chip: never show starlive_w / raw filenames. */
    private fun prettyLibLabel(item: WallpaperLibrary.Item, index: Int): String {
        val l = item.label.trim()
        return when {
            l.isBlank() -> "导入 ${index + 1}"
            l.contains("夜色") -> "夜色"
            l.startsWith("starlive", ignoreCase = true) -> "导入 ${index + 1}"
            l.endsWith(".jpg", true) || l.endsWith(".png", true) || l.endsWith(".jpeg", true) ->
                "导入 ${index + 1}"
            else -> l.take(8)
        }
    }

    private fun showMoreMenu() {
        val items = arrayOf(
            "兑换主题",
            "私人定制",
            "显示与恢复…",
            "规格说明",
            "升级到 Lyra",
            "关于",
        )
        AlertDialog.Builder(this)
            .setTitle("更多")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, RedeemActivity::class.java))
                    1 -> startActivity(Intent(this, CustomActivity::class.java))
                    2 -> showSettingsDialog()
                    3 -> startActivity(Intent(this, SpecActivity::class.java))
                    4 -> startActivity(Intent(this, UpgradeActivity::class.java))
                    5 -> startActivity(Intent(this, AboutActivity::class.java))
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showSettingsDialog() {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(4), dp(8), dp(8))
        }
        col.addView(buildSettingsCard())
        col.addView(
            TextView(this).apply {
                text = "日夜"
                setTextColor(UiTokens.textMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setPadding(0, dp(12), 0, dp(6))
            },
        )
        col.addView(buildNightRow())
        val scroll = ScrollView(this).apply {
            addView(col)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(420),
            )
        }
        AlertDialog.Builder(this)
            .setTitle("显示与恢复")
            .setView(scroll)
            .setPositiveButton("完成", null)
            .show()
    }

    private fun buildSettingsCard(): LinearLayout {
        val card = UiKit.card(this)
        card.addView(
            UiKit.settingRow(
                this,
                title = "空闲显示壁纸",
                subtitle = "关则让出原厂星环",
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
                subtitle = "双装时不抢星环",
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
                subtitle = "仅示范图轮换",
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
        nightRowHost = row
        val current = WallpaperRepository.nightMode(this)
        // 「自动」跟随车机显示模式（浅色1/深色2/自适应9），与 Lyra 远端日夜同源。
        // Short labels for dialog; full effective day/night still applied in backend.
        listOf("auto" to "自动", "dark" to "深色", "light" to "浅色").forEachIndexed { i, (mode, label) ->
            row.addView(
                UiKit.chip(this, label, selected = current == mode) {
                    WallpaperRepository.setNightMode(this, mode)
                    WallpaperRepository.invalidateStripCache()
                    // Force strip re-bake glass dissolve for ambient change.
                    sendBroadcast(
                        Intent(ClusterStripActivity.ACTION_RELOAD).setPackage(packageName),
                    )
                    if (orch.showing || orch.display.isAlive()) orch.applyCurrent("night-$mode")
                    rebuildNightRow()
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

    private fun rebuildNightRow() {
        val host = nightRowHost ?: return
        val parent = host.parent as? LinearLayout ?: return
        val idx = parent.indexOfChild(host)
        if (idx < 0) return
        parent.removeViewAt(idx)
        parent.addView(buildNightRow(), idx)
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
            // Mutual exclusive with library selection
            val selected = (active == d.id || selectedDemoId == d.id) &&
                !active.startsWith("lib:") && active != "custom"
            val thumb = loadDemoThumb(d)
            demoHost.addView(
                thumbChip(d.label, selected, thumb, onClick = {
                    selectedDemoId = d.id
                    WallpaperRepository.applyDemo(this, d.id)
                    rebuildDemoChips()
                    rebuildLibraryRail()
                    refreshUi()
                    Toast.makeText(this, "已选 ${d.label} · 点「应用上屏」", Toast.LENGTH_SHORT).show()
                }),
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
                    text = "暂无导入 · 点右上「导入」添加"
                    setTextColor(UiTokens.textMuted)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(6), 0, dp(6))
                },
            )
            return
        }
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val active = WallpaperRepository.activeId(this)
        items.forEachIndexed { index, item ->
            val selected = active == "lib:${item.id}" || active == item.id
            val label = prettyLibLabel(item, index)
            val thumb = loadLibraryThumb(item)
            row.addView(
                thumbChip(
                    label = label,
                    selected = selected,
                    thumb = thumb,
                    onClick = {
                        if (WallpaperRepository.applyLibraryItem(this, item.id)) {
                            selectedDemoId = null
                            rebuildDemoChips()
                            rebuildLibraryRail()
                            refreshUi()
                            Toast.makeText(this, "已选 $label · 点「应用上屏」", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLongClick = {
                        AlertDialog.Builder(this)
                            .setTitle("删除导入？")
                            .setMessage(label)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton("删除") { _, _ ->
                                WallpaperRepository.deleteLibraryItem(this, item.id)
                                rebuildLibraryRail()
                                refreshUi()
                            }
                            .show()
                        true
                    },
                ),
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
        // Prefer SAF OpenDocument (I1: multi-user car + ES/DocumentsUI).
        val opened = runCatching {
            pickDocument.launch(arrayOf("image/*", "image/jpeg", "image/png"))
            true
        }.getOrDefault(false)
        if (opened) return
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
        sourceTv.text = "当前 · ${WallpaperRepository.labelForActive(this)}"
        // Same bake as ClusterStrip (left edge dissolve + glass) so preview ≈ remote.
        if (::preview.isInitialized) {
            val night = WallpaperRepository.isNightish(this)
            val bmp = WallpaperRepository.decodeActiveForStrip(this, night)
            if (bmp != null) preview.setImageBitmap(bmp)
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

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
import android.view.MotionEvent
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Home: swipe gallery in hero + one primary apply. 导入/更多 on top bar.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var statusTv: TextView
    private lateinit var sourceTv: TextView
    private lateinit var heroSub: TextView
    private lateinit var pageIndicator: TextView
    private lateinit var galleryScroll: HorizontalScrollView
    private lateinit var galleryRow: LinearLayout
    private var galleryPageW: Int = 0
    private var galleryHeroH: Int = 0
    /** Index into [galleryLoop] (tripled list for infinite swipe). */
    private var galleryIndex: Int = 0
    /** Unique wallpapers (demos + library). */
    private var galleryBase: List<GalleryItem> = emptyList()
    /** [galleryBase] × 3 for seamless left/right loop. */
    private var galleryLoop: List<GalleryItem> = emptyList()
    private var snapPosted = false
    private var touchStartX = 0f

    private val orch get() = (application as StarLiveApp).orchestrator

    private var nightRowHost: LinearLayout? = null

    private data class GalleryItem(
        val key: String,
        val label: String,
        val demoId: String? = null,
        val libId: String? = null,
    )

    private val uiRefreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == StripOrchestrator.ACTION_UI_REFRESH) {
                runOnUiThread {
                    if (!::statusTv.isInitialized) return@runOnUiThread
                    val reason = intent.getStringExtra(StripOrchestrator.EXTRA_REASON).orEmpty()
                    if (reason.startsWith("ambient")) {
                        rebuildNightRow()
                        rebuildGallery(keepPage = true)
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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiTokens.bg)
            setPadding(dp(24), dp(16), dp(24), dp(24))
        }

        // 标题栏（导入 | 更多）→ 顶部横滑预览 → 应用上屏
        root.addView(buildTopBar())
        root.addView(buildHeroCard())
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
        rebuildGallery(keepPage = false)
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
        // 导入在「更多」左侧
        top.addView(
            UiKit.secondaryButton(this, "导入") { showImportSheet() }.also {
                it.minHeight = dp(40)
                it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { marginStart = dp(10) }
            },
        )
        top.addView(
            UiKit.ghostButton(this, "更多") { showMoreMenu() }.also {
                it.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { marginStart = dp(8) }
            },
        )
        return top
    }

    /** Full-width paging gallery: demos + library as swipeable strip previews. */
    private fun buildHeroCard(): LinearLayout {
        val card = UiKit.card(this)
        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(10) }

        val contentW = resources.displayMetrics.widthPixels - dp(48)
        galleryPageW = contentW
        galleryHeroH = (contentW * StripGeometry.WALLPAPER_H / StripGeometry.WALLPAPER_W)
            .coerceIn(dp(72), dp(128))

        galleryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                galleryHeroH,
            )
        }
        galleryScroll = object : HorizontalScrollView(this) {
            override fun fling(velocityX: Int) {
                // Page-sized fling so left/right both land on a neighbor (not free-scroll chaos).
                if (galleryPageW <= 0 || galleryLoop.isEmpty()) {
                    super.fling(velocityX)
                    return
                }
                val dir = when {
                    velocityX > 200 -> -1 // fling right → previous page
                    velocityX < -200 -> 1 // fling left → next page
                    else -> 0
                }
                if (dir == 0) {
                    super.fling(velocityX)
                    postSnapGallery()
                    return
                }
                val cur = ((scrollX + galleryPageW / 2f) / galleryPageW).roundToInt()
                    .coerceIn(0, galleryLoop.lastIndex)
                val target = (cur + dir).coerceIn(0, galleryLoop.lastIndex)
                smoothScrollTo(target * galleryPageW, 0)
                postDelayed({ snapGallery() }, 280)
            }
        }.apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_ALWAYS
            isFillViewport = false
            clipToOutline = true
            applyRoundedBg(UiTokens.heroBg, 14f, UiTokens.stroke)
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    val r = 14f * resources.displayMetrics.density
                    outline.setRoundRect(0, 0, view.width, view.height, r)
                }
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                galleryHeroH,
            )
            addView(galleryRow)
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        touchStartX = event.x
                        // Keep parent vertical ScrollView from stealing horizontal swipes.
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (abs(event.x - touchStartX) > dp(8)) {
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.parent?.requestDisallowInterceptTouchEvent(false)
                        postSnapGallery()
                    }
                }
                false
            }
            setOnScrollChangeListener { _, scrollX, _, _, _ ->
                if (galleryPageW > 0 && galleryLoop.isNotEmpty()) {
                    val page = ((scrollX + galleryPageW / 2f) / galleryPageW)
                        .toInt()
                        .coerceIn(0, galleryLoop.lastIndex)
                    updatePageIndicator(page)
                }
            }
        }
        card.addView(galleryScroll)

        pageIndicator = TextView(this).apply {
            setTextColor(UiTokens.textMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(0, dp(10), 0, 0)
            gravity = Gravity.CENTER
        }
        card.addView(pageIndicator)

        sourceTv = TextView(this).apply {
            setTextColor(UiTokens.textPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(0, dp(6), 0, 0)
            gravity = Gravity.CENTER
        }
        card.addView(sourceTv)

        heroSub = TextView(this).apply {
            setTextColor(UiTokens.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(4), 0, 0)
            setLineSpacing(0f, 1.2f)
            gravity = Gravity.CENTER
        }
        card.addView(heroSub)
        return card
    }

    private fun buildPrimaryApply(): Button =
        UiKit.primaryButton(this, "应用上屏") { applyWallpaper() }.also {
            it.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(14) }
            it.minHeight = dp(52)
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        }

    private fun buildGalleryList(): List<GalleryItem> {
        val out = mutableListOf<GalleryItem>()
        WallpaperRepository.demos(this).forEach { d ->
            out += GalleryItem(key = d.id, label = d.label, demoId = d.id)
        }
        WallpaperRepository.libraryItems(this).forEachIndexed { index, item ->
            out += GalleryItem(
                key = "lib:${item.id}",
                label = prettyLibLabel(item, index),
                libId = item.id,
            )
        }
        return out
    }

    private fun rebuildGallery(keepPage: Boolean) {
        if (!::galleryRow.isInitialized) return
        val prevKey = galleryLoop.getOrNull(galleryIndex)?.key
            ?: galleryBase.getOrNull(realIndexOf(galleryIndex))?.key
        galleryBase = buildGalleryList()
        // Triple list → middle copy is home; jump back when nearing either edge.
        galleryLoop = if (galleryBase.isEmpty()) {
            emptyList()
        } else {
            galleryBase + galleryBase + galleryBase
        }
        galleryScroll.post {
            val w = galleryScroll.width.takeIf { it > 0 }
                ?: (resources.displayMetrics.widthPixels - dp(48))
            val h = galleryScroll.height.takeIf { it > 0 }
                ?: galleryHeroH.coerceAtLeast(dp(72))
            galleryPageW = w
            galleryHeroH = h
            galleryRow.removeAllViews()
            galleryLoop.forEach { item ->
                val iv = ImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(w, h)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageBitmap(loadGalleryBitmap(item))
                    contentDescription = item.label
                    setOnClickListener { applyWallpaper() }
                    setOnLongClickListener {
                        if (item.libId != null) {
                            confirmDeleteLibrary(item)
                            true
                        } else {
                            false
                        }
                    }
                }
                galleryRow.addView(iv)
            }
            if (galleryBase.isEmpty()) {
                galleryIndex = 0
                pageIndicator.text = "暂无壁纸 · 点右上角导入"
                sourceTv.text = "—"
                return@post
            }
            val n = galleryBase.size
            // Prefer middle block so user can swipe both ways immediately.
            var real = when {
                keepPage && prevKey != null ->
                    galleryBase.indexOfFirst { it.key == prevKey }.takeIf { it >= 0 }
                else -> null
            }
            if (real == null) {
                val active = WallpaperRepository.activeId(this)
                real = galleryBase.indexOfFirst {
                    it.key == active || it.demoId == active || it.key == "lib:$active"
                }.takeIf { it >= 0 } ?: 0
            }
            galleryIndex = n + real.coerceIn(0, n - 1)
            galleryScroll.scrollTo(galleryIndex * galleryPageW, 0)
            applyGallerySelection(galleryIndex, toast = false, recenter = false)
        }
    }

    private fun realIndexOf(loopIndex: Int): Int {
        val n = galleryBase.size
        if (n <= 0) return 0
        return ((loopIndex % n) + n) % n
    }

    private fun loadGalleryBitmap(item: GalleryItem): Bitmap? {
        val night = WallpaperRepository.isNightish(this)
        return runCatching {
            when {
                item.demoId != null -> {
                    val demo = WallpaperRepository.demos(this).firstOrNull { it.id == item.demoId }
                        ?: return@runCatching null
                    assets.open(demo.assetFor(night)).use { input ->
                        val raw = BitmapFactory.decodeStream(input) ?: return@runCatching null
                        scaleToBand(raw)
                    }
                }
                item.libId != null -> {
                    val lib = WallpaperRepository.libraryItems(this).firstOrNull { it.id == item.libId }
                        ?: return@runCatching null
                    val f = lib.file(this)
                    if (!f.isFile) return@runCatching null
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val raw = BitmapFactory.decodeFile(f.absolutePath, opts) ?: return@runCatching null
                    scaleToBand(raw)
                }
                else -> null
            }
        }.getOrNull()
    }

    private fun scaleToBand(raw: Bitmap): Bitmap {
        val tw = StripGeometry.WALLPAPER_W
        val th = StripGeometry.WALLPAPER_H
        if (raw.width == tw && raw.height == th) return raw
        return Bitmap.createScaledBitmap(raw, tw, th, true).also {
            if (it !== raw) raw.recycle()
        }
    }

    private fun postSnapGallery() {
        if (snapPosted) return
        snapPosted = true
        galleryScroll.post {
            snapPosted = false
            snapGallery()
        }
    }

    private fun snapGallery() {
        if (galleryPageW <= 0 || galleryLoop.isEmpty() || galleryBase.isEmpty()) return
        val page = ((galleryScroll.scrollX + galleryPageW / 2f) / galleryPageW)
            .roundToInt()
            .coerceIn(0, galleryLoop.lastIndex)
        galleryScroll.smoothScrollTo(page * galleryPageW, 0)
        if (page != galleryIndex) {
            applyGallerySelection(page, toast = true, recenter = true)
        } else {
            // Still recenter if sitting on edge copies.
            recenterLoopIfNeeded(page)
            updatePageIndicator(page)
        }
    }

    /**
     * Keep scroll position in the middle third of the tripled list so both
     * directions stay open (infinite loop without a dead end).
     */
    private fun recenterLoopIfNeeded(loopIndex: Int) {
        val n = galleryBase.size
        if (n <= 1 || galleryPageW <= 0) return
        val real = realIndexOf(loopIndex)
        val mid = n + real
        // Near first or third copy → jump to middle copy (no animation).
        if (loopIndex < n || loopIndex >= n * 2) {
            galleryIndex = mid
            galleryScroll.post {
                galleryScroll.scrollTo(mid * galleryPageW, 0)
            }
        } else {
            galleryIndex = loopIndex
        }
    }

    private fun applyGallerySelection(page: Int, toast: Boolean, recenter: Boolean) {
        if (galleryLoop.isEmpty() || galleryBase.isEmpty()) return
        val loopIdx = page.coerceIn(0, galleryLoop.lastIndex)
        val item = galleryLoop[loopIdx]
        when {
            item.demoId != null -> WallpaperRepository.applyDemo(this, item.demoId)
            item.libId != null -> WallpaperRepository.applyLibraryItem(this, item.libId)
        }
        if (recenter) {
            recenterLoopIfNeeded(loopIdx)
        } else {
            galleryIndex = loopIdx
        }
        updatePageIndicator(galleryIndex)
        refreshUi()
        if (toast) {
            Toast.makeText(this, item.label, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePageIndicator(page: Int) {
        if (!::pageIndicator.isInitialized || galleryBase.isEmpty()) return
        val real = realIndexOf(page)
        val item = galleryBase[real]
        pageIndicator.text = "左右滑动 · ${real + 1} / ${galleryBase.size} · 循环"
        if (::sourceTv.isInitialized) {
            sourceTv.text = item.label
        }
    }

    private fun confirmDeleteLibrary(item: GalleryItem) {
        val libId = item.libId ?: return
        AlertDialog.Builder(this)
            .setTitle("删除导入？")
            .setMessage(item.label)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("删除") { _, _ ->
                WallpaperRepository.deleteLibraryItem(this, libId)
                rebuildGallery(keepPage = false)
                refreshUi()
            }
            .show()
    }

    /** Human-readable library label. */
    private fun prettyLibLabel(item: WallpaperLibrary.Item, index: Int): String {
        val l = item.label.trim()
        return when {
            l.isBlank() -> "导入 ${index + 1}"
            l.contains("夜色") -> "夜色"
            l.startsWith("starlive", ignoreCase = true) -> "导入 ${index + 1}"
            l.endsWith(".jpg", true) || l.endsWith(".png", true) || l.endsWith(".jpeg", true) ->
                "导入 ${index + 1}"
            else -> l.take(10)
        }
    }

    private fun showMoreMenu() {
        val items = arrayOf(
            "兑换主题",
            "私人定制",
            "显示与恢复…",
            "恢复示范",
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
                    3 -> confirmRestoreDemo()
                    4 -> startActivity(Intent(this, SpecActivity::class.java))
                    5 -> startActivity(Intent(this, UpgradeActivity::class.java))
                    6 -> startActivity(Intent(this, AboutActivity::class.java))
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
        rebuildGallery(keepPage = true)
        refreshUi()
        maybeSoftHints()
        if (::statusTv.isInitialized) {
            statusTv.postDelayed({ if (!isFinishing) refreshUi() }, 900L)
            statusTv.postDelayed({ if (!isFinishing) refreshUi() }, 3_200L)
        }
    }

    override fun onPause() {
        runCatching { unregisterReceiver(uiRefreshReceiver) }
        super.onPause()
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
                rebuildGallery(keepPage = false)
                refreshUi()
                Toast.makeText(this, R.string.restore_done, Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun refreshUi() {
        if (!::sourceTv.isInitialized) return
        if (galleryBase.isNotEmpty()) {
            updatePageIndicator(galleryIndex)
        } else {
            sourceTv.text = WallpaperRepository.labelForActive(this)
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
                "星环与预览一致 · 点预览或「应用上屏」可再同步",
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
                "已选「${WallpaperRepository.labelForActive(this)}」· 点「应用上屏」同步到星环",
            )
        }
    }

    private fun styleStatus(label: String, color: Int, sub: String) {
        statusTv.text = label
        statusTv.setTextColor(color)
        heroSub.text = sub
    }
}

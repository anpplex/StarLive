package com.starlive.app.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
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
import android.view.VelocityTracker
import android.view.View
import android.view.animation.PathInterpolator
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
    /** Page index at gesture start — settle is always start ± 1 (never skip). */
    private var gestureStartIndex: Int = 0
    private var gestureStartX: Float = 0f
    private var lastTouchX: Float = 0f
    private var pageAnimator: ValueAnimator? = null
    private var velocityTracker: VelocityTracker? = null
    private var galleryDragging = false

    private val orch get() = (application as StarLiveApp).orchestrator

    companion object {
        /** px/s — car HU flings are hot; keep threshold firm so a nudge doesn't page. */
        private const val FLING_VX = 700f
        /** Fraction of page width to commit next/prev without fling. */
        private const val PAGE_COMMIT_FRAC = 0.15f
        private val PAGE_EASE = PathInterpolator(0.22f, 1f, 0.36f, 1f)
    }

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

        // 标题栏（导入 | 更多）→ 顶部横滑预览 → 应用到星环
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
        // Version lives in 关于 — keep home chrome clean.
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
                // Free fling skips pages; paging is settled in onTouchEvent.
            }

            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                // Always own horizontal gallery gestures (both directions).
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        cancelPageAnim()
                        galleryDragging = false
                        velocityTracker?.recycle()
                        velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
                        gestureStartX = ev.x
                        lastTouchX = ev.x
                        // Prefer index from scroll so reverse/forward both see true start page.
                        gestureStartIndex = pageIndexFromScroll(scrollX)
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        velocityTracker?.addMovement(ev)
                        if (abs(ev.x - gestureStartX) > dp(6)) {
                            galleryDragging = true
                            parent?.requestDisallowInterceptTouchEvent(true)
                            return true
                        }
                    }
                }
                return super.onInterceptTouchEvent(ev) || galleryDragging
            }

            override fun onTouchEvent(ev: MotionEvent): Boolean {
                if (galleryPageW <= 0 || galleryLoop.isEmpty()) {
                    return super.onTouchEvent(ev)
                }
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        cancelPageAnim()
                        galleryDragging = true
                        velocityTracker?.recycle()
                        velocityTracker = VelocityTracker.obtain().also { it.addMovement(ev) }
                        gestureStartX = ev.x
                        lastTouchX = ev.x
                        gestureStartIndex = pageIndexFromScroll(scrollX)
                        // Lock to page origin so drag delta is clean both ways.
                        scrollTo(gestureStartIndex * galleryPageW, 0)
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        velocityTracker?.addMovement(ev)
                        parent?.requestDisallowInterceptTouchEvent(true)
                        // Finger left → content left → next; finger right → previous.
                        val dx = lastTouchX - ev.x
                        lastTouchX = ev.x
                        val maxScroll = (galleryLoop.size - 1) * galleryPageW
                        val next = (scrollX + dx).toInt().coerceIn(0, maxScroll.coerceAtLeast(0))
                        scrollTo(next, 0)
                        updatePageIndicator(pageIndexFromScroll(next))
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        velocityTracker?.addMovement(ev)
                        velocityTracker?.computeCurrentVelocity(1000)
                        val vx = velocityTracker?.xVelocity ?: 0f
                        velocityTracker?.recycle()
                        velocityTracker = null
                        parent?.requestDisallowInterceptTouchEvent(false)
                        galleryDragging = false
                        settleGalleryFromGesture(fingerDx = ev.x - gestureStartX, velocityX = vx)
                        return true
                    }
                }
                return true
            }
        }.apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            isFillViewport = false
            clipToOutline = true
            isClickable = true
            isFocusable = true
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
        UiKit.primaryButton(this, getString(R.string.btn_apply)) { applyWallpaper() }.also {
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
        cancelPageAnim()
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
                pageIndicator.text = "暂无壁纸"
                sourceTv.text = "点右上角导入"
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
            gestureStartIndex = galleryIndex
            galleryScroll.scrollTo(galleryIndex * galleryPageW, 0)
            applyGallerySelection(galleryIndex, recenter = false)
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

    private fun pageIndexFromScroll(scrollX: Int): Int {
        if (galleryPageW <= 0 || galleryLoop.isEmpty()) return 0
        return ((scrollX + galleryPageW / 2f) / galleryPageW)
            .toInt()
            .coerceIn(0, galleryLoop.lastIndex)
    }

    /**
     * One gesture → at most one neighbor page.
     * Direction from **finger** delta (not free-scroll midpoint):
     * - finger left / fling left  → next (1→2→3→4→1)
     * - finger right / fling right → prev (1→4→3→2→1)
     */
    private fun settleGalleryFromGesture(fingerDx: Float, velocityX: Float) {
        if (galleryPageW <= 0 || galleryLoop.isEmpty() || galleryBase.isEmpty()) return
        val commitPx = galleryPageW * PAGE_COMMIT_FRAC
        // fingerDx: + = finger moved right → previous page
        // velocityX: + = fling right → previous page
        val dir = when {
            velocityX < -FLING_VX -> 1
            velocityX > FLING_VX -> -1
            fingerDx < -commitPx -> 1
            fingerDx > commitPx -> -1
            else -> 0
        }
        // Stay in middle copy: start is always ~[n, 2n); ±1 never hits hard edge.
        val start = gestureStartIndex.coerceIn(0, galleryLoop.lastIndex)
        val target = (start + dir).coerceIn(0, galleryLoop.lastIndex)
        animateToPage(target)
    }

    private fun cancelPageAnim() {
        pageAnimator?.cancel()
        pageAnimator = null
    }

    /** Damped page settle (ease-out). Duration scales lightly with distance. */
    private fun animateToPage(targetIndex: Int) {
        if (!::galleryScroll.isInitialized || galleryPageW <= 0) return
        val safeTarget = targetIndex.coerceIn(0, galleryLoop.lastIndex)
        cancelPageAnim()
        val from = galleryScroll.scrollX
        val to = safeTarget * galleryPageW
        if (abs(from - to) <= 1) {
            galleryScroll.scrollTo(to, 0)
            applyGallerySelection(safeTarget, recenter = true)
            return
        }
        val dist = abs(to - from).toFloat()
        val duration = (260L + (dist * 0.12f).toLong()).coerceIn(280L, 480L)
        pageAnimator = ValueAnimator.ofInt(from, to).apply {
            this.duration = duration
            interpolator = PAGE_EASE
            addUpdateListener { anim ->
                val x = anim.animatedValue as Int
                galleryScroll.scrollTo(x, 0)
                updatePageIndicator(pageIndexFromScroll(x))
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    pageAnimator = null
                    applyGallerySelection(safeTarget, recenter = true)
                }

                override fun onAnimationCancel(animation: Animator) {
                    pageAnimator = null
                }
            })
            start()
        }
    }

    /**
     * Always park in the **middle** third of the tripled list so both
     * previous and next gestures stay open (infinite L/R without a wall).
     */
    private fun parkInMiddleCopy(loopIndex: Int) {
        val n = galleryBase.size
        if (n <= 0 || galleryPageW <= 0) return
        val real = realIndexOf(loopIndex)
        val mid = if (n == 1) 0 else n + real
        galleryIndex = mid
        gestureStartIndex = mid
        if (galleryScroll.scrollX != mid * galleryPageW) {
            galleryScroll.scrollTo(mid * galleryPageW, 0)
        }
    }

    private fun applyGallerySelection(page: Int, recenter: Boolean) {
        if (galleryLoop.isEmpty() || galleryBase.isEmpty()) return
        val loopIdx = page.coerceIn(0, galleryLoop.lastIndex)
        val item = galleryLoop[loopIdx]
        when {
            item.demoId != null -> WallpaperRepository.applyDemo(this, item.demoId)
            item.libId != null -> WallpaperRepository.applyLibraryItem(this, item.libId)
        }
        if (recenter) {
            parkInMiddleCopy(loopIdx)
        } else {
            galleryIndex = loopIdx
            gestureStartIndex = loopIdx
        }
        updatePageIndicator(galleryIndex)
        refreshUi()
    }

    private fun updatePageIndicator(page: Int) {
        if (!::pageIndicator.isInitialized || galleryBase.isEmpty()) return
        val real = realIndexOf(page)
        val item = galleryBase[real]
        pageIndicator.text = "${real + 1} / ${galleryBase.size}"
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
            "显示设置",
            "恢复内置壁纸",
            "兑换主题",
            "壁纸定制",
            "升级 Lyra",
            "壁纸规格",
            "关于作者",
            "关于",
        )
        AlertDialog.Builder(this)
            .setTitle("更多")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> showSettingsDialog()
                    1 -> confirmRestoreDemo()
                    2 -> startActivity(Intent(this, RedeemActivity::class.java))
                    3 -> startActivity(Intent(this, CustomActivity::class.java))
                    4 -> startActivity(Intent(this, UpgradeActivity::class.java))
                    5 -> startActivity(Intent(this, SpecActivity::class.java))
                    6 -> startActivity(Intent(this, AuthorActivity::class.java))
                    7 -> startActivity(Intent(this, AboutActivity::class.java))
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
                text = "外观"
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
            .setTitle("显示设置")
            .setView(scroll)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun buildSettingsCard(): LinearLayout {
        val card = UiKit.card(this)
        card.addView(
            UiKit.settingRow(
                this,
                title = "星环壁纸",
                subtitle = "关闭后恢复原厂星环显示",
                initial = WallpaperRepository.idlePrefer(this),
            ) { checked ->
                orch.setIdlePrefer(checked)
                refreshUi()
                Toast.makeText(
                    this,
                    if (checked) "已开启星环壁纸" else "已恢复原厂显示",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
        card.addView(UiKit.spacer(this, 6))
        card.addView(
            UiKit.settingRow(
                this,
                title = "Lyra 优先",
                subtitle = "已安装 Lyra 时由 Lyra 占用星环",
                initial = WallpaperRepository.yieldWhenLyraInstalled(this),
            ) { checked ->
                WallpaperRepository.setYieldWhenLyraInstalled(this, checked)
                orch.refreshLyraHandoff("user-toggle")
                refreshUi()
                Toast.makeText(
                    this,
                    if (checked) "已开启 Lyra 优先" else "已关闭 Lyra 优先",
                    Toast.LENGTH_SHORT,
                ).show()
            },
        )
        card.addView(UiKit.spacer(this, 6))
        card.addView(
            UiKit.settingRow(
                this,
                title = "内置轮播",
                subtitle = "在内置壁纸之间自动切换",
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
        // 跟随系统 = 车机显示模式（浅色 / 深色 / 自适应）。
        listOf("auto" to "跟随系统", "dark" to "深色", "light" to "浅色").forEachIndexed { i, (mode, label) ->
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
                getString(R.string.status_closed),
                UiTokens.textMuted,
                getString(R.string.hero_closed),
            )
            alive -> styleStatus(
                getString(R.string.status_showing),
                UiTokens.success,
                "",
            )
            launching -> styleStatus(
                getString(R.string.status_launching),
                UiTokens.info,
                "",
            )
            orch.lastError == "launch-failed" -> styleStatus(
                getString(R.string.status_fail),
                UiTokens.danger,
                ClusterApplyMessages.noCluster(orch.display.listDisplaysForProbe()),
            )
            else -> styleStatus(
                getString(R.string.status_hidden),
                UiTokens.warning,
                getString(R.string.hero_hidden),
            )
        }
    }

    private fun styleStatus(label: String, color: Int, sub: String) {
        statusTv.text = label
        statusTv.setTextColor(color)
        if (sub.isBlank()) {
            heroSub.visibility = View.GONE
            heroSub.text = ""
        } else {
            heroSub.visibility = View.VISIBLE
            heroSub.text = sub
        }
    }
}

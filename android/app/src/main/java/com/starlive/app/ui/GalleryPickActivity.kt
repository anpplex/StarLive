package com.starlive.app.ui

import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.starlive.app.ui.UiTokens.applyRoundedBg
import com.starlive.app.ui.UiTokens.dp
import com.starlive.ring.StripGeometry
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max

/**
 * MediaStore image picker tuned for car HU:
 * - Fast thumbs (LruCache + small decode / loadThumbnail)
 * - Folder chips (BUCKET)
 * - Strip-like wallpapers sorted first (≈2990×284)
 * - Pagination (page size [PAGE_SIZE])
 */
class GalleryPickActivity : AppCompatActivity() {
    private val io = Executors.newFixedThreadPool(6)
    private val thumbJobs = HashMap<Long, Future<*>>()
    private val thumbGen = AtomicInteger(0)

    private lateinit var grid: GridView
    private lateinit var emptyTv: TextView
    private lateinit var progress: ProgressBar
    private lateinit var countTv: TextView
    private lateinit var folderRow: LinearLayout
    private lateinit var moreBtn: TextView

    /** Full catalog for current folder filter (metadata only). */
    private var catalog = listOf<MediaItem>()
    /** Visible page items. */
    private val visible = mutableListOf<MediaItem>()
    private var page = 0
    /** null = 全部; [BUCKET_STRIP_ONLY] = 仅长条; else MediaStore BUCKET_ID */
    private var selectedBucket: String? = null
    private var folderChips = listOf<FolderChip>()
    private var adapter: ThumbAdapter? = null
    private var cellW = 0
    private var cellH = 0

    private data class MediaItem(
        val id: Long,
        val uri: Uri,
        val name: String,
        val width: Int,
        val height: Int,
        val bucketId: String,
        val bucketName: String,
        val dateModified: Long,
        val stripScore: Int,
    ) {
        /** Strong strip / panoramic wallpaper candidate. */
        val isStripLike: Boolean get() = stripScore >= STRIP_FILTER_MIN
    }

    private data class FolderChip(
        val bucketId: String?,
        val label: String,
        val count: Int,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = dp(16)
        val gap = dp(10)
        val columns = 5
        cellW = (resources.displayMetrics.widthPixels - pad * 2 - gap * (columns - 1)) / columns
        cellH = (cellW * 0.52f).toInt().coerceAtLeast(dp(68))

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UiTokens.bg)
            setPadding(pad, dp(12), pad, dp(12))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(
            UiKit.title(this, "选择图片").also {
                it.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            },
        )
        top.addView(UiKit.ghostButton(this, "取消") { finish() })
        root.addView(top)

        countTv = TextView(this).apply {
            setTextColor(UiTokens.textMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(0, dp(6), 0, dp(6))
            text = "正在加载车机媒体库…"
        }
        root.addView(countTv)

        val folderScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dp(8))
        }
        folderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        folderScroll.addView(folderRow)
        root.addView(folderScroll)

        progress = ProgressBar(this).apply { isIndeterminate = true }
        root.addView(
            progress,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).also { it.gravity = Gravity.CENTER_HORIZONTAL },
        )

        emptyTv = TextView(this).apply {
            setTextColor(UiTokens.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(dp(24), dp(40), dp(24), dp(24))
            setLineSpacing(0f, 1.25f)
            text = "未在媒体库中找到图片。\n可改用「从文件选择」，或将图片放入 Download。"
        }
        root.addView(emptyTv)

        grid = GridView(this).apply {
            numColumns = columns
            horizontalSpacing = gap
            verticalSpacing = gap
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            clipToPadding = false
            setSelector(android.R.color.transparent)
            setOnScrollListener(object : AbsListView.OnScrollListener {
                override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) = Unit
                override fun onScroll(
                    view: AbsListView?,
                    firstVisibleItem: Int,
                    visibleItemCount: Int,
                    totalItemCount: Int,
                ) {
                    if (totalItemCount == 0) return
                    // Prefetch next page near end.
                    if (firstVisibleItem + visibleItemCount >= totalItemCount - columns) {
                        maybeLoadMore(auto = true)
                    }
                }
            })
        }
        root.addView(
            grid,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f),
        )

        moreBtn = TextView(this).apply {
            text = "加载更多"
            gravity = Gravity.CENTER
            setTextColor(UiTokens.textLink)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setPadding(dp(12), dp(12), dp(12), dp(8))
            visibility = View.GONE
            setOnClickListener { maybeLoadMore(auto = false) }
        }
        root.addView(moreBtn)

        setContentView(root)
        loadCatalog()
    }

    override fun onDestroy() {
        thumbGen.incrementAndGet()
        thumbJobs.values.forEach { it.cancel(true) }
        thumbJobs.clear()
        io.shutdownNow()
        super.onDestroy()
    }

    private fun loadCatalog() {
        progress.visibility = View.VISIBLE
        emptyTv.visibility = View.GONE
        grid.visibility = View.GONE
        moreBtn.visibility = View.GONE
        val gen = thumbGen.incrementAndGet()
        thread {
            val all = queryAllMetadata()
            if (gen != thumbGen.get()) return@thread
            val folders = buildFolders(all)
            runOnUiThread {
                if (gen != thumbGen.get()) return@runOnUiThread
                progress.visibility = View.GONE
                folderChips = folders
                rebuildFolderChips()
                applyFilterAndResetPage()
            }
        }
    }

    private fun rebuildFolderChips() {
        folderRow.removeAllViews()
        folderChips.forEach { chip ->
            val selected = selectedBucket == chip.bucketId
            folderRow.addView(
                UiKit.chip(this, "${chip.label} ${chip.count}", selected = selected) {
                    selectedBucket = chip.bucketId
                    rebuildFolderChips()
                    applyFilterAndResetPage()
                }.also {
                    it.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { marginEnd = dp(8) }
                },
            )
        }
    }

    private fun applyFilterAndResetPage() {
        catalog = when (selectedBucket) {
            null -> allCatalogSorted
            BUCKET_STRIP_ONLY -> allCatalogSorted.filter { it.isStripLike }
            else -> allCatalogSorted.filter { it.bucketId == selectedBucket }
        }
        page = 0
        visible.clear()
        adapter = ThumbAdapter()
        grid.adapter = adapter
        grid.setOnItemClickListener { _, _, position, _ ->
            val item = visible.getOrNull(position) ?: return@setOnItemClickListener
            startActivity(
                ImportConfirmActivity.intentFromUri(this, item.uri, item.name.ifBlank { "图库" }),
            )
            finish()
        }
        appendPage()
        updateChrome()
    }

    private var allCatalogSorted: List<MediaItem> = emptyList()

    private fun applyFilterSource(all: List<MediaItem>) {
        allCatalogSorted = all
    }

    private fun appendPage() {
        val from = page * PAGE_SIZE
        if (from >= catalog.size) {
            updateChrome()
            return
        }
        val to = minOf(from + PAGE_SIZE, catalog.size)
        val slice = catalog.subList(from, to)
        val start = visible.size
        visible.addAll(slice)
        page++
        adapter?.notifyDataSetChanged()
        // Warm first screen thumbs.
        slice.take(15).forEach { prefetchThumb(it) }
        if (start == 0 && visible.isEmpty()) {
            emptyTv.visibility = View.VISIBLE
            grid.visibility = View.GONE
        } else {
            emptyTv.visibility = View.GONE
            grid.visibility = View.VISIBLE
        }
        updateChrome()
    }

    private fun maybeLoadMore(auto: Boolean) {
        if (visible.size >= catalog.size) return
        if (auto && page > 0 && visible.size < PAGE_SIZE) return
        appendPage()
    }

    private fun updateChrome() {
        val folderLabel = folderChips.firstOrNull { it.bucketId == selectedBucket }?.label ?: "全部"
        val stripOnly = selectedBucket == BUCKET_STRIP_ONLY
        val stripN = if (stripOnly) {
            catalog.size
        } else {
            catalog.count { it.isStripLike }
        }
        countTv.text = buildString {
            append(folderLabel)
            append(" · ")
            append(visible.size)
            append("/")
            append(catalog.size)
            append(" 张")
            if (stripOnly) {
                append(" · 仅星环长条")
            } else if (stripN > 0) {
                append(" · 长条 $stripN")
            }
            append(" · 点选导入")
        }
        moreBtn.visibility = if (visible.size < catalog.size) View.VISIBLE else View.GONE
        moreBtn.text = "加载更多（${catalog.size - visible.size}）"
        emptyTv.text = if (stripOnly && catalog.isEmpty()) {
            "当前没有识别为长条壁纸的图片。\n可切回「全部」，或选择接近 2990×284 / 全条 4032 的图。"
        } else {
            "未在媒体库中找到图片。\n可改用「从文件选择」，或将图片放入 Download。"
        }
    }

    private fun buildFolders(all: List<MediaItem>): List<FolderChip> {
        val map = linkedMapOf<String, Pair<String, Int>>()
        all.forEach { item ->
            val key = item.bucketId.ifBlank { "_other" }
            val name = item.bucketName.ifBlank { "其它" }
            val cur = map[key]
            map[key] = name to ((cur?.second ?: 0) + 1)
        }
        val stripCount = all.count { it.isStripLike }
        val chips = mutableListOf(
            FolderChip(null, "全部", all.size),
            FolderChip(BUCKET_STRIP_ONLY, "仅长条", stripCount),
        )
        map.entries
            .sortedByDescending { it.value.second }
            .take(16)
            .forEach { (id, pair) ->
                chips += FolderChip(id, shortenFolder(pair.first), pair.second)
            }
        return chips
    }

    private fun shortenFolder(name: String): String =
        when {
            name.equals("Download", true) || name.contains("下载") -> "Download"
            name.equals("Pictures", true) || name.contains("图片") -> "Pictures"
            name.equals("DCIM", true) -> "DCIM"
            name.equals("Camera", true) -> "相机"
            name.length > 10 -> name.take(9) + "…"
            else -> name
        }

    private fun queryAllMetadata(): List<MediaItem> {
        val out = ArrayList<MediaItem>(512)
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
        )
        // Pull a generous catalog; display is paginated.
        val sort = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
        runCatching {
            contentResolver.query(collection, projection, null, null, sort)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val wCol = c.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val hCol = c.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val sizeCol = c.getColumnIndex(MediaStore.Images.Media.SIZE)
                val mimeCol = c.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val bucketIdCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = c.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val dateCol = c.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED)
                var n = 0
                while (c.moveToNext() && n < MAX_CATALOG) {
                    val size = if (sizeCol >= 0) c.getLong(sizeCol) else 1L
                    if (size in 1 until 64L) continue
                    val mime = if (mimeCol >= 0) c.getString(mimeCol).orEmpty() else "image/*"
                    if (mime.startsWith("video/")) continue
                    val id = c.getLong(idCol)
                    val name = c.getString(nameCol).orEmpty()
                    val w = if (wCol >= 0) c.getInt(wCol) else 0
                    val h = if (hCol >= 0) c.getInt(hCol) else 0
                    val bucketId = if (bucketIdCol >= 0) c.getString(bucketIdCol).orEmpty() else ""
                    val bucketName = if (bucketNameCol >= 0) c.getString(bucketNameCol).orEmpty() else ""
                    val date = if (dateCol >= 0) c.getLong(dateCol) else 0L
                    val uri = ContentUris.withAppendedId(collection, id)
                    out += MediaItem(
                        id = id,
                        uri = uri,
                        name = name,
                        width = w,
                        height = h,
                        bucketId = bucketId,
                        bucketName = bucketName,
                        dateModified = date,
                        stripScore = stripScore(w, h, name),
                    )
                    n++
                }
            }
        }.onFailure {
            android.util.Log.w(TAG, "MediaStore query failed", it)
            runOnUiThread {
                Toast.makeText(this, "读取媒体库失败", Toast.LENGTH_SHORT).show()
            }
        }
        // Strip-like first, then recent.
        val sorted = out.sortedWith(
            compareByDescending<MediaItem> { it.stripScore }
                .thenByDescending { it.dateModified },
        )
        applyFilterSource(sorted)
        return sorted
    }

    /**
     * Higher = better wallpaper-band candidate.
     * Target band ≈ 2990×284 (ratio ~10.5).
     */
    private fun stripScore(w: Int, h: Int, name: String): Int {
        if (w <= 0 || h <= 0) {
            // Unknown size: light boost for name hints.
            return if (nameHintStrip(name)) 20 else 0
        }
        val ratio = w.toFloat() / h.toFloat()
        val target = StripGeometry.WALLPAPER_W.toFloat() / StripGeometry.WALLPAPER_H
        var score = 0
        // Near exact band.
        if (abs(w - StripGeometry.WALLPAPER_W) <= 4 && abs(h - StripGeometry.WALLPAPER_H) <= 4) {
            score += 100
        }
        // Full cluster strip.
        if (w >= StripGeometry.STRIP_W - 4 && h in 200..400) score += 80
        // Wide panoramic (ratio close to band).
        if (ratio >= 6f && h in 160..480) score += 60
        if (abs(ratio - target) < 1.5f && w >= 1800) score += 40
        if (ratio >= 4f && w >= 1200) score += 20
        if (nameHintStrip(name)) score += 15
        return score
    }

    private fun nameHintStrip(name: String): Boolean {
        val n = name.lowercase()
        return n.contains("wallpaper") || n.contains("cluster") ||
            n.contains("starlive") || n.contains("lyra") ||
            n.contains("2990") || n.contains("4032") || n.contains("strip")
    }

    private fun prefetchThumb(item: MediaItem) {
        if (ThumbCache.get(item.id) != null) return
        if (thumbJobs[item.id]?.isDone == false) return
        val gen = thumbGen.get()
        thumbJobs[item.id] = io.submit {
            if (gen != thumbGen.get()) return@submit
            val bmp = loadThumb(item) ?: return@submit
            if (gen != thumbGen.get()) {
                bmp.recycle()
                return@submit
            }
            ThumbCache.put(item.id, bmp)
        }
    }

    private inner class ThumbAdapter : BaseAdapter() {
        override fun getCount(): Int = visible.size
        override fun getItem(position: Int): MediaItem = visible[position]
        override fun getItemId(position: Int): Long = visible[position].id

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val cell = (convertView as? ImageView) ?: ImageView(this@GalleryPickActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                applyRoundedBg(UiTokens.surface2, 10f, UiTokens.stroke)
                layoutParams = ViewGroup.LayoutParams(cellW, cellH)
            }
            val item = visible[position]
            cell.tag = item.id
            val cached = ThumbCache.get(item.id)
            if (cached != null && !cached.isRecycled) {
                cell.setImageBitmap(cached)
            } else {
                cell.setImageBitmap(null)
                cell.setBackgroundColor(UiTokens.surface2)
                val gen = thumbGen.get()
                val job = io.submit {
                    if (gen != thumbGen.get()) return@submit
                    val bmp = loadThumb(item) ?: return@submit
                    if (gen != thumbGen.get()) {
                        bmp.recycle()
                        return@submit
                    }
                    ThumbCache.put(item.id, bmp)
                    runOnUiThread {
                        if (cell.tag == item.id && !bmp.isRecycled) {
                            cell.setImageBitmap(bmp)
                        }
                    }
                }
                thumbJobs[item.id] = job
            }
            // Prefetch next few.
            for (i in 1..4) {
                visible.getOrNull(position + i)?.let { prefetchThumb(it) }
            }
            return cell
        }
    }

    private fun loadThumb(item: MediaItem): Bitmap? {
        return runCatching {
            // Tiny thumbs for grid speed on HU.
            val tw = 160
            val th = 90
            if (Build.VERSION.SDK_INT >= 29) {
                contentResolver.loadThumbnail(item.uri, Size(tw, th), null)
            } else {
                @Suppress("DEPRECATION")
                val mini = MediaStore.Images.Thumbnails.getThumbnail(
                    contentResolver,
                    item.id,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null,
                )
                if (mini != null) return@runCatching mini
                val sample = sampleSize(item.width, item.height, tw, th)
                contentResolver.openInputStream(item.uri)?.use { input ->
                    val opts = BitmapFactory.Options().apply {
                        inSampleSize = max(sample, 4)
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }
                    BitmapFactory.decodeStream(input, null, opts)
                }
            }
        }.getOrNull()
    }

    private fun sampleSize(w: Int, h: Int, reqW: Int, reqH: Int): Int {
        if (w <= 0 || h <= 0) return 8
        var sample = 1
        while (w / sample > reqW * 2 || h / sample > reqH * 2) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    /** Process-wide thumb cache so reopen is snappy. */
    private object ThumbCache {
        private val cache = object : LruCache<Long, Bitmap>(
            // ~8MB
            (8 * 1024 * 1024) / 1024,
        ) {
            override fun sizeOf(key: Long, value: Bitmap): Int =
                value.byteCount / 1024
        }

        fun get(id: Long): Bitmap? = synchronized(cache) { cache.get(id) }
        fun put(id: Long, bmp: Bitmap) = synchronized(cache) { cache.put(id, bmp) }
    }

    companion object {
        private const val TAG = "StarLive"
        private const val MAX_CATALOG = 800
        private const val PAGE_SIZE = 40
        /** Sentinel chip id: filter strip-like only (not a MediaStore bucket). */
        private const val BUCKET_STRIP_ONLY = "__strip_only__"
        /** Min stripScore for「仅长条」chip (see stripScore()). */
        private const val STRIP_FILTER_MIN = 40
    }
}

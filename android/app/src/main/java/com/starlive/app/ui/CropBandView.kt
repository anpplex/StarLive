package com.starlive.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.starlive.ring.StripGeometry
import kotlin.math.max
import kotlin.math.min

/**
 * Interactive crop for star-ring wallpaper band (aspect [StripGeometry.WALLPAPER_W]×[StripGeometry.WALLPAPER_H]).
 * Drag to pan, pinch or [zoomBy] to scale; crop window is fixed in the view.
 */
class CropBandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private var source: Bitmap? = null
    private val drawMatrix = Matrix()
    private val invertMatrix = Matrix()
    private val cropRect = RectF()
    private val imageRect = RectF()
    private val mappedImage = RectF()
    private val tmp = FloatArray(2)

    private var minScale = 1f
    private var maxScale = 6f
    private var currentScale = 1f

    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 0, 0, 0)
        style = Paint.Style.FILL
    }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 76, 126, 240)
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
    }
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 1f * resources.displayMetrics.density
    }
    private val bmpPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    var onTransformChanged: (() -> Unit)? = null

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                val focusX = detector.focusX
                val focusY = detector.focusY
                applyScale(factor, focusX, focusY)
                return true
            }
        },
    )

    fun setSourceBitmap(bmp: Bitmap?) {
        source = bmp
        post { resetToCover() }
        invalidate()
    }

    fun sourceWidth(): Int = source?.width ?: 0
    fun sourceHeight(): Int = source?.height ?: 0

    /** Current scale relative to fit-cover min scale. */
    fun zoomFactor(): Float =
        if (minScale <= 0f) 1f else (currentScale / minScale).coerceAtLeast(0.01f)

    fun resetToCover() {
        val bmp = source ?: return
        if (width <= 0 || height <= 0) return
        layoutCropRect()
        imageRect.set(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
        val scaleX = cropRect.width() / bmp.width
        val scaleY = cropRect.height() / bmp.height
        minScale = max(scaleX, scaleY)
        maxScale = minScale * 5.5f
        currentScale = minScale
        drawMatrix.reset()
        drawMatrix.postScale(currentScale, currentScale)
        // Center image on crop window.
        val dx = cropRect.centerX() - bmp.width * currentScale / 2f
        val dy = cropRect.centerY() - bmp.height * currentScale / 2f
        drawMatrix.postTranslate(dx, dy)
        clampTranslation()
        invalidate()
        onTransformChanged?.invoke()
    }

    fun zoomBy(factor: Float) {
        if (source == null || width == 0) return
        applyScale(factor, cropRect.centerX(), cropRect.centerY())
    }

    /**
     * Export crop window content as 2990×284 wallpaper band.
     */
    fun exportCropped(): Bitmap? {
        val bmp = source ?: return null
        if (!drawMatrix.invert(invertMatrix)) return null
        val corners = floatArrayOf(
            cropRect.left, cropRect.top,
            cropRect.right, cropRect.top,
            cropRect.right, cropRect.bottom,
            cropRect.left, cropRect.bottom,
        )
        invertMatrix.mapPoints(corners)
        var minX = corners[0]
        var minY = corners[1]
        var maxX = corners[0]
        var maxY = corners[1]
        for (i in 0 until 4) {
            val x = corners[i * 2]
            val y = corners[i * 2 + 1]
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
        }
        // Axis-aligned crop in source (matrix is similarity: scale+translate only).
        val sx = minX.coerceIn(0f, bmp.width - 1f)
        val sy = minY.coerceIn(0f, bmp.height - 1f)
        val ex = maxX.coerceIn(sx + 1f, bmp.width.toFloat())
        val ey = maxY.coerceIn(sy + 1f, bmp.height.toFloat())
        val x = sx.toInt().coerceIn(0, bmp.width - 1)
        val y = sy.toInt().coerceIn(0, bmp.height - 1)
        val w = (ex - sx).toInt().coerceIn(1, bmp.width - x)
        val h = (ey - sy).toInt().coerceIn(1, bmp.height - y)
        val piece = Bitmap.createBitmap(bmp, x, y, w, h)
        val tw = StripGeometry.WALLPAPER_W
        val th = StripGeometry.WALLPAPER_H
        if (piece.width == tw && piece.height == th) return piece
        return Bitmap.createScaledBitmap(piece, tw, th, true).also {
            if (it !== piece) piece.recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (source != null) resetToCover() else layoutCropRect()
    }

    private fun layoutCropRect() {
        if (width <= 0 || height <= 0) return
        val aspect = StripGeometry.WALLPAPER_W.toFloat() / StripGeometry.WALLPAPER_H
        val pad = 4f * resources.displayMetrics.density
        var cw = width - pad * 2
        var ch = cw / aspect
        if (ch > height - pad * 2) {
            ch = height - pad * 2
            cw = ch * aspect
        }
        val left = (width - cw) / 2f
        val top = (height - ch) / 2f
        cropRect.set(left, top, left + cw, top + ch)
    }

    private fun applyScale(factor: Float, focusX: Float, focusY: Float) {
        val target = (currentScale * factor).coerceIn(minScale, maxScale)
        val real = target / currentScale
        if (real == 1f) return
        drawMatrix.postScale(real, real, focusX, focusY)
        currentScale = target
        clampTranslation()
        invalidate()
        onTransformChanged?.invoke()
    }

    private fun clampTranslation() {
        val bmp = source ?: return
        imageRect.set(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
        drawMatrix.mapRect(mappedImage, imageRect)
        var dx = 0f
        var dy = 0f
        if (mappedImage.width() <= cropRect.width()) {
            dx = cropRect.centerX() - mappedImage.centerX()
        } else {
            if (mappedImage.left > cropRect.left) dx = cropRect.left - mappedImage.left
            if (mappedImage.right < cropRect.right) dx = cropRect.right - mappedImage.right
        }
        if (mappedImage.height() <= cropRect.height()) {
            dy = cropRect.centerY() - mappedImage.centerY()
        } else {
            if (mappedImage.top > cropRect.top) dy = cropRect.top - mappedImage.top
            if (mappedImage.bottom < cropRect.bottom) dy = cropRect.bottom - mappedImage.bottom
        }
        if (dx != 0f || dy != 0f) {
            drawMatrix.postTranslate(dx, dy)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bmp = source
        canvas.drawColor(UiTokens.heroBg)
        if (bmp != null && !bmp.isRecycled) {
            canvas.drawBitmap(bmp, drawMatrix, bmpPaint)
        }
        // Dim outside crop.
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, dimPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), dimPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, dimPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, dimPaint)
        canvas.drawRect(cropRect, framePaint)
        // Rule of thirds guides.
        val gw = cropRect.width() / 3f
        val gh = cropRect.height() / 3f
        canvas.drawLine(cropRect.left + gw, cropRect.top, cropRect.left + gw, cropRect.bottom, guidePaint)
        canvas.drawLine(cropRect.left + gw * 2, cropRect.top, cropRect.left + gw * 2, cropRect.bottom, guidePaint)
        canvas.drawLine(cropRect.left, cropRect.top + gh, cropRect.right, cropRect.top + gh, guidePaint)
        canvas.drawLine(cropRect.left, cropRect.top + gh * 2, cropRect.right, cropRect.top + gh * 2, guidePaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                dragging = true
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && dragging && event.pointerCount == 1) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    lastX = event.x
                    lastY = event.y
                    drawMatrix.postTranslate(dx, dy)
                    clampTranslation()
                    invalidate()
                    onTransformChanged?.invoke()
                } else if (event.pointerCount > 1) {
                    lastX = event.x
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = false
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }
}

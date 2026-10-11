package com.example.modity.ui.statistics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import com.example.modity.utils.themeColor
import com.google.android.material.R as MaterialR

/** Shared drawing setup for the two minimal charts. */
abstract class ChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    protected val density = resources.displayMetrics.density
    protected val labelHeight = 20 * density
    protected val topPadding = 8 * density

    protected val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.themeColor(MaterialR.attr.colorOnSurface)
        strokeWidth = 2 * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    protected val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.themeColor(MaterialR.attr.colorOutlineVariant)
        strokeWidth = density
    }
    protected val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.themeColor(MaterialR.attr.colorOnSurfaceVariant)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics)
        textAlign = Paint.Align.CENTER
    }

    protected var labels: List<String> = emptyList()

    protected val chartBottom: Float get() = height - labelHeight
    protected val chartHeight: Float get() = chartBottom - topPadding

    protected fun slotCenter(index: Int, count: Int): Float {
        val slot = width.toFloat() / count
        return slot * index + slot / 2
    }

    /** Draws at most ~8 labels so they never overlap. */
    protected fun drawLabels(canvas: Canvas) {
        if (labels.isEmpty()) return
        val step = ((labels.size + MAX_LABELS - 1) / MAX_LABELS).coerceAtLeast(1)
        labels.forEachIndexed { index, label ->
            if (index % step == 0 || index == labels.lastIndex) {
                canvas.drawText(label, slotCenter(index, labels.size), height - 4 * density, labelPaint)
            }
        }
    }

    private companion object {
        const val MAX_LABELS = 8
    }
}

/** Bar per day, e.g. completed tasks. */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ChartView(context, attrs) {
    private var values: List<Float> = emptyList()
    private val rect = RectF()

    fun setData(values: List<Float>, labels: List<String>) {
        this.values = values
        this.labels = labels
        contentDescription = values.joinToString { it.toInt().toString() }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawLine(0f, chartBottom, width.toFloat(), chartBottom, gridPaint)
        if (values.isEmpty()) return
        val max = values.max().coerceAtLeast(1f)
        val slot = width.toFloat() / values.size
        val barWidth = (slot * 0.5f).coerceAtMost(18 * density)
        val radius = barWidth / 2
        values.forEachIndexed { index, value ->
            if (value <= 0f) return@forEachIndexed
            val center = slotCenter(index, values.size)
            val top = chartBottom - chartHeight * (value / max)
            rect.set(center - barWidth / 2, top, center + barWidth / 2, chartBottom)
            canvas.drawRoundRect(rect, radius, radius, mainPaint)
        }
        drawLabels(canvas)
    }
}

/** Percentage line (0–100); null values are gaps (days without tasks). */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ChartView(context, attrs) {
    private var values: List<Float?> = emptyList()
    private val path = Path()
    private val linePaint = Paint(mainPaint).apply { style = Paint.Style.STROKE }

    fun setData(values: List<Float?>, labels: List<String>) {
        this.values = values
        this.labels = labels
        contentDescription = values.joinToString { it?.toInt()?.toString() ?: "-" }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        listOf(0f, 0.5f, 1f).forEach { fraction ->
            val y = chartBottom - chartHeight * fraction
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }
        if (values.isEmpty()) return
        path.reset()
        var drawing = false
        values.forEachIndexed { index, value ->
            if (value == null) {
                drawing = false
                return@forEachIndexed
            }
            val x = slotCenter(index, values.size)
            val y = chartBottom - chartHeight * (value / 100f)
            if (drawing) path.lineTo(x, y) else path.moveTo(x, y)
            drawing = true
            canvas.drawCircle(x, y, 2.5f * density, mainPaint)
        }
        canvas.drawPath(path, linePaint)
        drawLabels(canvas)
    }
}

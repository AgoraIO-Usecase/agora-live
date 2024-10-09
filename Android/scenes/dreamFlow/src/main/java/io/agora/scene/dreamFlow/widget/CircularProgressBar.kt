package io.agora.scene.dreamFlow.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View


class CircularProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var progressColor: Int = Color.BLUE
    private var backgroundTintColor: Int = Color.LTGRAY
    private var strokeWidth: Float = 20f
    private var progress: Float = 0f // Progress in percentage (0 to 100)

    init {
        // Initialize attributes if needed
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val radius = Math.min(width, height) / 2 - strokeWidth / 2
        val centerX = width / 2
        val centerY = height / 2

        // Draw background circle
        paint.color = backgroundTintColor
        paint.strokeWidth = strokeWidth
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Draw progress circle
        paint.color = progressColor
        val sweepAngle = (progress / 100) * 360
        canvas.drawArc(
            centerX - radius, centerY - radius,
            centerX + radius, centerY + radius,
            -90f, sweepAngle, false, paint
        )
    }

    fun setProgress(progress: Float) {
        this.progress = progress.coerceIn(0f, 100f)
        invalidate() // Redraw the view
    }

    fun setProgressColor(color: Int) {
        this.progressColor = color
        invalidate()
    }

    fun setBackgroundTintColor(color: Int) {
        this.backgroundTintColor = color
        invalidate()
    }

    fun setStrokeWidth(width: Float) {
        this.strokeWidth = width
        invalidate()
    }
}
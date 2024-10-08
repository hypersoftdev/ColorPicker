package com.hyeprsoft.picker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.hyeprsoft.colorpicker.R

class ColorPickerView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    interface OnColorChangeListener {
        fun onColorChanged(color: Int)
        fun onHexColorChanged(hexColor: String) {}
    }

    private var colorChangeListener: OnColorChangeListener? = null
    private var parentBitmap: Bitmap? = null

    // Paint for draggable circle (existing one)
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(minStrokeWidth) // Default stroke width in pixels
        color = Color.BLACK // Default color
    }

    // Paint for fixed outer circle (new one)
    private val outerCirclePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(10f)
        color = Color.RED
    }

    private val matrix = Matrix()
    private var isDragging = false
    private var lastX = 0f
    private var lastY = 0f

    private var fixedWidth = dpToPx(100f)
    private var fixedHeight = dpToPx(100f)
    var circleRadius = fixedWidth / 2f
        private set
    private var changeStrokeColor = true

    // Define min and max limits
    private val minCircleRadius = dpToPx(30f)
    private val maxCircleRadius = dpToPx(200f)
    private val minStrokeWidth = dpToPx(10f)
    private val maxStrokeWidth = dpToPx(50f)

    init {
        // Load custom attributes from XML
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.DraggableImageView,
            0, 0
        ).apply {
            try {
                // Retrieve and set the stroke width from XML with limits
                setStrokeWidth(getDimension(R.styleable.DraggableImageView_strokeWidth, dpToPx(5f)))
                setStrokeColor(getColor(R.styleable.DraggableImageView_strokeColor, Color.BLACK))
                setCircleRadius(getDimension(R.styleable.DraggableImageView_circleRadius, dpToPx(50f)))
                setChangeStrokeColorRealTime(getBoolean(R.styleable.DraggableImageView_changeStrokeColor, true))
                setOuterStrokeColor(getColor(R.styleable.DraggableImageView_outerCircleColor, Color.BLACK))

            } finally {
                recycle() // Always recycle the TypedArray after use
            }
        }

        post {
            centerCircle()
            invalidate()  // Redraw the view to reflect the initial position
        }

        if (isInEditMode) {
            invalidate()
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    private fun centerCircle() {
        val centerX = width / 2f
        val centerY = height / 2f
        matrix.reset()
        matrix.postTranslate(centerX - fixedWidth / 2f, centerY - fixedHeight / 2f)
    }


    fun setOuterStrokeColor(color: Int) {
        outerCirclePaint.color = color
        invalidate()
    }

    fun setChangeStrokeColorRealTime(change: Boolean) {
        changeStrokeColor = change
    }

    fun setStrokeWidth(strokeWidth: Float) {
        paint.strokeWidth = strokeWidth.coerceIn(minStrokeWidth, maxStrokeWidth) // Enforce limits
        invalidate() // Refresh the view to reflect changes
    }

    fun setStrokeColor(color: Int) {
        paint.color = color
        invalidate() // Refresh the view to reflect changes
    }

    fun setCircleRadius(radius: Float) {
        circleRadius = radius.coerceIn(minCircleRadius, maxCircleRadius) // Enforce limits
        fixedWidth = circleRadius * 2f
        fixedHeight = circleRadius * 2f
        centerCircle() // Re-center the circle after changing the radius
        invalidate() // Refresh the view to reflect changes
    }

    fun setOnColorChangeListener(listener: OnColorChangeListener) {
        this.colorChangeListener = listener
    }

    private fun captureParentView() {
        val parentView = this.parent as? ViewGroup ?: return

        parentBitmap = Bitmap.createBitmap(parentView.width, parentView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(parentBitmap!!)
        parentView.draw(canvas) // Draw the parent view into the bitmap
    }

    private fun updateCircleColorFromParent(x: Float, y: Float) {
        parentBitmap?.let { bitmap ->
            if (x >= 0 && x < bitmap.width && y >= 0 && y < bitmap.height) {
                val pixelColor = bitmap.getPixel(x.toInt(), y.toInt())
                if (changeStrokeColor) paint.color = pixelColor

                // Notify the listener about the new color
                colorChangeListener?.onColorChanged(pixelColor)

                // Notify the listener about the hex color if overridden
                colorChangeListener?.onHexColorChanged(String.format("#%06X", (0xFFFFFF and pixelColor)))
            }
        }
    }

    private fun updateCircleColorFromParent() {
        parentBitmap?.let { bitmap ->
            // Calculate the exact center of the inner circle
            val circlePosition = floatArrayOf(0f, 0f)
            matrix.mapPoints(circlePosition)
            val centerX = circlePosition[0] + circleRadius
            val centerY = circlePosition[1] + circleRadius

            // Ensure the center is within the bitmap bounds
            if (centerX >= 0 && centerX < bitmap.width && centerY >= 0 && centerY < bitmap.height) {
                val pixelColor = bitmap.getPixel(centerX.toInt(), centerY.toInt())
                if (changeStrokeColor) paint.color = pixelColor

                // Notify the listener about the new color
                colorChangeListener?.onColorChanged(pixelColor)

                // Notify the listener about the hex color if overridden
                colorChangeListener?.onHexColorChanged(String.format("#%06X", (0xFFFFFF and pixelColor)))
            }
        }
    }



    private fun isTouchInsideCircle(x: Float, y: Float): Boolean {
        val circlePosition = floatArrayOf(0f, 0f)
        matrix.mapPoints(circlePosition)
        val circleCenterX = circlePosition[0] + circleRadius
        val circleCenterY = circlePosition[1] + circleRadius

        val distance = Math.sqrt(Math.pow((x - circleCenterX).toDouble(), 2.0) + Math.pow((y - circleCenterY).toDouble(), 2.0))
        return distance <= circleRadius
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isInEditMode) {
            // Just draw a simple circle for preview mode
            canvas.drawCircle(width / 2f, height / 2f, circleRadius, paint)
            return
        }

        if (parentBitmap == null) {
            captureParentView()
        }

        val circlePosition = floatArrayOf(0f, 0f)
        matrix.mapPoints(circlePosition)

        // Draw the fixed outer circle
        canvas.drawCircle(circlePosition[0] + circleRadius, circlePosition[1] + circleRadius, circleRadius + outerCirclePaint.strokeWidth / 2, outerCirclePaint)

        // Draw the draggable inner circle
        canvas.drawCircle(circlePosition[0] + circleRadius, circlePosition[1] + circleRadius, circleRadius, paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerCircle() // Re-center the circle when the size changes
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                isDragging = isTouchInsideCircle(event.x, event.y)
                if (isDragging) {
                    updateCircleColorFromParent()
                    return true
                }
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    matrix.postTranslate(dx, dy)
                    lastX = event.x
                    lastY = event.y

                    updateCircleColorFromParent()
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    isDragging = false
                    return true
                }
            }
        }
        return false
    }


}






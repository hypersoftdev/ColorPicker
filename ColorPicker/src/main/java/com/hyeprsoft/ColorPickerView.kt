package com.hyeprsoft

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
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(5f) // Default stroke width in pixels
        color = Color.BLACK // Default color
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

            } finally {
                recycle() // Always recycle the TypedArray after use
            }
        }

        // Center the circle or image when the view is drawn for the first time
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

    fun setChangeStrokeColorRealTime(change:Boolean)
    {
        changeStrokeColor = change
    }

    // Method to set the stroke width and refresh the view
    fun setStrokeWidth(strokeWidth: Float) {
        paint.strokeWidth = strokeWidth.coerceIn(minStrokeWidth, maxStrokeWidth) // Enforce limits
        invalidate() // Refresh the view to reflect changes
    }

    // Method to set the stroke color and refresh the view
    fun setStrokeColor(color: Int) {
        paint.color = color
        invalidate() // Refresh the view to reflect changes
    }

    // Method to set the circle radius and refresh the view
    fun setCircleRadius(radius: Float) {
        circleRadius = radius.coerceIn(minCircleRadius, maxCircleRadius) // Enforce limits
        fixedWidth = circleRadius * 2f
        fixedHeight = circleRadius * 2f
        centerCircle() // Re-center the circle after changing the radius
        invalidate() // Refresh the view to reflect changes
    }

    // Set the listener for color changes
    fun setOnColorChangeListener(listener: OnColorChangeListener) {
        this.colorChangeListener = listener
    }

    // Method to capture the parent view into a Bitmap
    private fun captureParentView() {
        val parentView = this.parent as? ViewGroup ?: return

        parentBitmap = Bitmap.createBitmap(parentView.width, parentView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(parentBitmap!!)
        parentView.draw(canvas) // Draw the parent view into the bitmap
    }

    // Method to update the circle's color based on the parent view's content
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

    // Check if the touch is within the circle
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

        // Capture the parent view when the view is first drawn
        if (parentBitmap == null) {
            captureParentView()
        }

        val circlePosition = floatArrayOf(0f, 0f)
        matrix.mapPoints(circlePosition)
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
                // Only allow dragging if the touch is within the circle
                isDragging = isTouchInsideCircle(event.x, event.y)
                if (isDragging) {
                    // Update the color only when the touch is inside the circle
                    updateCircleColorFromParent(event.x, event.y)
                    return true // Handle the touch event
                }
                return false // Let the parent handle the touch event if not inside the circle
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    matrix.postTranslate(dx, dy)
                    lastX = event.x
                    lastY = event.y

                    // Update the color based on the new position
                    updateCircleColorFromParent(event.x, event.y)
                    invalidate()
                    return true // Handle the touch event
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    isDragging = false
                    return true // Handle the touch event
                }
            }
        }
        return false // Let the parent handle other events
    }

    fun getColorName(hex: String): String {
        // Map of hex values to color names
        val colorMap = mapOf(
            "#FF0000" to "Red",
            "#00FF00" to "Green",
            "#0000FF" to "Blue",
            "#FFFF00" to "Yellow",
            "#FFA500" to "Orange",
            "#800080" to "Purple",
            "#FFFFFF" to "White",
            "#000000" to "Black",
            "#FFC0CB" to "Pink",
            "#808080" to "Gray",
            "#A52A2A" to "Brown",
            "#FFD700" to "Gold",
            "#C0C0C0" to "Silver",
            "#008080" to "Teal",
            "#000080" to "Navy",
            "#FF4500" to "Orange Red",
            "#DA70D6" to "Orchid",
            "#B22222" to "Firebrick",
            "#5F9EA0" to "Cadet Blue",
            "#D2691E" to "Chocolate",
            "#7FFF00" to "Chartreuse",
            "#DDA0DD" to "Plum",
            "#FF1493" to "Deep Pink",
            "#00CED1" to "Dark Turquoise",
            "#FF6347" to "Tomato",
            "#4682B4" to "Steel Blue",
            "#B8860B" to "Dark Golden Rod",
            "#F08080" to "Light Coral",
            "#FF69B4" to "Hot Pink",
            "#00BFFF" to "Deep Sky Blue",
            "#7CFC00" to "Lawn Green",
            "#ADFF2F" to "Green Yellow",
            "#C71585" to "Medium Violet Red",
            "#F0E68C" to "Khaki",
            "#FFB6C1" to "Light Pink",
            "#FFE4E1" to "Misty Rose",
            "#E6E6FA" to "Lavender",
            "#FFF0F5" to "Lavender Blush",
            "#F5F5DC" to "Beige",
            "#DCDCDC" to "Gainsboro",
            "#F5FFFA" to "Mint Cream",
            "#FFEFD5" to "Papaya Whip",
            "#FFF5EE" to "Seashell",
            "#FFDEAD" to "Navajo White",
            "#FF8C00" to "Dark Orange",
            "#FFDAB9" to "Peach Puff",
            "#FFE4B5" to "Moccasin",
            "#FFE4C4" to "Bisque",
            "#7B68EE" to "Medium Slate Blue",
            "#4169E1" to "Royal Blue",
            "#8A2BE2" to "Blue Violet",
            "#4B0082" to "Indigo",
            "#8B008B" to "Dark Magenta",
            "#9932CC" to "Dark Orchid",
            "#9400D3" to "Dark Violet",
            "#6A5ACD" to "Slate Blue",
            "#FF00FF" to "Magenta",
            "#FF7F50" to "Coral",
            "#CD5C5C" to "Indian Red",
            "#F08080" to "Light Coral",
            "#FFD700" to "Gold",
            "#FFFFE0" to "Light Yellow",
            "#E0FFFF" to "Light Cyan",
            "#AFEEEE" to "Pale Turquoise",
            "#7FFFD4" to "Aquamarine",
            "#40E0D0" to "Turquoise",
            "#48D1CC" to "Medium Turquoise",
            "#20B2AA" to "Light Sea Green",
            "#3CB371" to "Medium Sea Green",
            "#2E8B57" to "Sea Green",
            "#228B22" to "Forest Green",
            "#008000" to "Green",
            "#006400" to "Dark Green",
            "#66CDAA" to "Medium Aquamarine",
            "#B0E0E6" to "Powder Blue",
            "#ADD8E6" to "Light Blue",
            "#B0C4DE" to "Light Steel Blue",
            "#4682B4" to "Steel Blue",
            "#6495ED" to "Cornflower Blue",
            "#00BFFF" to "Deep Sky Blue",
            "#1E90FF" to "Dodger Blue",
            "#00CED1" to "Dark Turquoise",
            "#5F9EA0" to "Cadet Blue",
            "#87CEFA" to "Light Sky Blue",
            "#87CEEB" to "Sky Blue",
            "#00FFFF" to "Cyan",
            "#0000CD" to "Medium Blue",
            "#00008B" to "Dark Blue",
            "#000080" to "Navy",
            "#8B4513" to "Saddle Brown",
            "#A0522D" to "Sienna",
            "#D2691E" to "Chocolate",
            "#CD853F" to "Peru",
            "#BC8F8F" to "Rosy Brown",
            "#F4A460" to "Sandy Brown",
            "#FFD700" to "Gold",
            "#FFFFF0" to "Ivory",
            "#F5DEB3" to "Wheat",
            "#FFE4C4" to "Bisque",
            "#F5F5F5" to "White Smoke",
            "#DCDCDC" to "Gainsboro",
            "#D3D3D3" to "Light Gray",
            "#C0C0C0" to "Silver",
            "#A9A9A9" to "Dark Gray",
            "#808080" to "Gray",
            "#696969" to "Dim Gray",
            "#778899" to "Light Slate Gray",
            "#708090" to "Slate Gray",
            "#2F4F4F" to "Dark Slate Gray",
            "#00FFFF" to "Cyan",
            "#F0FFFF" to "Azure",
            "#F5FFFA" to "Mint Cream",
            "#FFE4B5" to "Moccasin",
            "#FFEBCD" to "Blanched Almond",
            "#FFE4E1" to "Misty Rose",
            "#FFF5EE" to "Seashell",
            "#F5F5DC" to "Beige",
            "#F0E68C" to "Khaki",
            "#D3D3D3" to "Light Gray",
            "#C0C0C0" to "Silver",
            "#A9A9A9" to "Dark Gray",
            "#808080" to "Gray",
            "#696969" to "Dim Gray",
            "#778899" to "Light Slate Gray",
            "#708090" to "Slate Gray",
            "#2F4F4F" to "Dark Slate Gray",
            "#B0C4DE" to "Light Steel Blue",
            "#B22222" to "Firebrick",
            "#CD5C5C" to "Indian Red",
            "#FF4500" to "Orange Red",
            "#DC143C" to "Crimson",
            "#FF6347" to "Tomato",
            "#FF7F50" to "Coral",
            "#FF8C00" to "Dark Orange",
            "#FFA07A" to "Light Salmon",
            "#FF69B4" to "Hot Pink",
            "#FFB6C1" to "Light Pink",
            "#FF1493" to "Deep Pink",
            "#FFC0CB" to "Pink",
            "#DDA0DD" to "Plum",
            "#DA70D6" to "Orchid",
            "#EE82EE" to "Violet",
            "#9400D3" to "Dark Violet",
            "#8A2BE2" to "Blue Violet",
            "#6A5ACD" to "Slate Blue",
            "#7B68EE" to "Medium Slate Blue",
            "#4169E1" to "Royal Blue",
            "#4682B4" to "Steel Blue",
            "#5F9EA0" to "Cadet Blue",
            "#00CED1" to "Dark Turquoise",
            "#20B2AA" to "Light Sea Green",
            "#3CB371" to "Medium Sea Green",
            "#2E8B57" to "Sea Green",
            "#228B22" to "Forest Green",
            "#008000" to "Green",
            "#006400" to "Dark Green",
            "#66CDAA" to "Medium Aquamarine",
            "#40E0D0" to "Turquoise",
            "#48D1CC" to "Medium Turquoise",
            "#00BFFF" to "Deep Sky Blue",
            "#1E90FF" to "Dodger Blue",
            "#7B68EE" to "Medium Slate Blue",
            "#87CEFA" to "Light Sky Blue",
            "#87CEEB" to "Sky Blue",
            "#B0E0E6" to "Powder Blue",
            "#ADD8E6" to "Light Blue",
            "#B0C4DE" to "Light Steel Blue",
            "#F0F8FF" to "Alice Blue",
            "#F5F5FF" to "Ghost White",
            "#F0FFFF" to "Azure",
            "#F5FFFA" to "Mint Cream",
            "#FFFFE0" to "Light Yellow",
            "#FFF0F5" to "Lavender Blush",
            "#FFE4E1" to "Misty Rose",
            "#F5DEB3" to "Wheat",
            "#FFEBCD" to "Blanched Almond",
            "#FFB6C1" to "Light Pink",
            "#FFE4B5" to "Moccasin",
            "#FF6347" to "Tomato",
            "#FF7F50" to "Coral",
            "#FF8C00" to "Dark Orange",
            "#FFD700" to "Gold",
            "#FFFF00" to "Yellow",
            "#FFFFE0" to "Light Yellow",
            "#F5F5DC" to "Beige",
            "#E0FFFF" to "Light Cyan",
            "#B0E0E6" to "Powder Blue",
            "#ADD8E6" to "Light Blue",
            "#B0C4DE" to "Light Steel Blue",
            "#4682B4" to "Steel Blue",
            "#6495ED" to "Cornflower Blue",
            "#00BFFF" to "Deep Sky Blue",
            "#1E90FF" to "Dodger Blue",
            "#87CEFA" to "Light Sky Blue",
            "#87CEEB" to "Sky Blue",
            "#00FFFF" to "Cyan",
            "#0000CD" to "Medium Blue",
            "#00008B" to "Dark Blue",
            "#000080" to "Navy",
            "#8B4513" to "Saddle Brown",
            "#A0522D" to "Sienna",
            "#D2691E" to "Chocolate",
            "#CD853F" to "Peru",
            "#BC8F8F" to "Rosy Brown",
            "#F4A460" to "Sandy Brown",
            "#FFD700" to "Gold",
            "#FFFFF0" to "Ivory",
            "#F5DEB3" to "Wheat",
            "#FFE4C4" to "Bisque",
            "#F5F5F5" to "White Smoke",
            "#DCDCDC" to "Gainsboro",
            "#D3D3D3" to "Light Gray",
            "#C0C0C0" to "Silver",
            "#A9A9A9" to "Dark Gray",
            "#808080" to "Gray",
            "#696969" to "Dim Gray",
            "#778899" to "Light Slate Gray",
            "#708090" to "Slate Gray",
            "#2F4F4F" to "Dark Slate Gray",
            "#B0C4DE" to "Light Steel Blue",
            "#B22222" to "Firebrick",
            "#CD5C5C" to "Indian Red",
            "#FF4500" to "Orange Red",
            "#DC143C" to "Crimson",
            "#FF6347" to "Tomato",
            "#FF7F50" to "Coral",
            "#FF8C00" to "Dark Orange",
            "#FFA07A" to "Light Salmon",
            "#FF69B4" to "Hot Pink",
            "#FFB6C1" to "Light Pink",
            "#FF1493" to "Deep Pink",
            "#FFC0CB" to "Pink",
            "#DDA0DD" to "Plum",
            "#DA70D6" to "Orchid",
            "#EE82EE" to "Violet",
            "#9400D3" to "Dark Violet",
            "#8A2BE2" to "Blue Violet",
            "#6A5ACD" to "Slate Blue",
            "#7B68EE" to "Medium Slate Blue",
            "#4169E1" to "Royal Blue",
            "#4682B4" to "Steel Blue",
            "#5F9EA0" to "Cadet Blue",
            "#00CED1" to "Dark Turquoise",
            "#20B2AA" to "Light Sea Green",
            "#3CB371" to "Medium Sea Green",
            "#2E8B57" to "Sea Green",
            "#228B22" to "Forest Green",
            "#008000" to "Green",
            "#006400" to "Dark Green",
            "#66CDAA" to "Medium Aquamarine",
            "#40E0D0" to "Turquoise",
            "#48D1CC" to "Medium Turquoise",
            "#00BFFF" to "Deep Sky Blue",
            "#1E90FF" to "Dodger Blue",
            "#7B68EE" to "Medium Slate Blue",
            "#87CEFA" to "Light Sky Blue",
            "#87CEEB" to "Sky Blue",
            "#B0E0E6" to "Powder Blue",
            "#ADD8E6" to "Light Blue",
            "#B0C4DE" to "Light Steel Blue",
            "#F0F8FF" to "Alice Blue",
            "#F5F5FF" to "Ghost White",
            "#F0FFFF" to "Azure",
            "#F5FFFA" to "Mint Cream",
            "#FFFFE0" to "Light Yellow",
            "#FFF0F5" to "Lavender Blush",
            "#FFE4E1" to "Misty Rose",
            "#F5DEB3" to "Wheat",
            "#FFEBCD" to "Blanched Almond",
            "#FFB6C1" to "Light Pink",
            "#FFE4B5" to "Moccasin",
            "#FF6347" to "Tomato",
            "#FF7F50" to "Coral",
            "#FF8C00" to "Dark Orange",
            "#FFD700" to "Gold",
            "#FFFF00" to "Yellow",
            "#FFFFE0" to "Light Yellow",
            "#F5F5DC" to "Beige",
            "#E0FFFF" to "Light Cyan",
            "#B0E0E6" to "Powder Blue",
            "#ADD8E6" to "Light Blue",
            "#B0C4DE" to "Light Steel Blue",
            "#4682B4" to "Steel Blue",
            "#6495ED" to "Cornflower Blue",
            "#00BFFF" to "Deep Sky Blue",
            "#1E90FF" to "Dodger Blue",
            "#87CEFA" to "Light Sky Blue",
            "#87CEEB" to "Sky Blue",
            "#00FFFF" to "Cyan",
            "#0000CD" to "Medium Blue",
            "#00008B" to "Dark Blue",
            "#000080" to "Navy",
            "#8B4513" to "Saddle Brown",
            "#A0522D" to "Sienna",
            "#D2691E" to "Chocolate",
            "#CD853F" to "Peru",
            "#BC8F8F" to "Rosy Brown",
            "#F4A460" to "Sandy Brown",
            "#FFD700" to "Gold",
            "#FFFFF0" to "Ivory",
            "#F5DEB3" to "Wheat",
            "#FFE4C4" to "Bisque",
            "#F5F5F5" to "White Smoke",
            "#DCDCDC" to "Gainsboro",
            "#D3D3D3" to "Light Gray",
            "#C0C0C0" to "Silver",
            "#A9A9A9" to "Dark Gray",
            "#808080" to "Gray",
            "#696969" to "Dim Gray",
            "#778899" to "Light Slate Gray",
            "#708090" to "Slate Gray",
            "#2F4F4F" to "Dark Slate Gray",
            "#B0C4DE" to "Light Steel Blue",
            "#B22222" to "Firebrick",
            "#CD5C5C" to "Indian Red",
            "#FF4500" to "Orange Red",
            "#DC143C" to "Crimson",
            "#FF6347" to "Tomato",
            "#FF7F50" to "Coral",
            "#FF8C00" to "Dark Orange",
            "#FFA07A" to "Light Salmon",
            "#FF69B4" to "Hot Pink",
            "#FFB6C1" to "Light Pink",
            "#FF1493" to "Deep Pink",
            "#FFC0CB" to "Pink",
            "#DDA0DD" to "Plum",
            "#DA70D6" to "Orchid",
            "#EE82EE" to "Violet",
            "#9400D3" to "Dark Violet",
            "#8A2BE2" to "Blue Violet",
            "#6A5ACD" to "Slate Blue",
            "#7B68EE" to "Medium Slate Blue",
            "#4169E1" to "Royal Blue",
            "#4682B4" to "Steel Blue",
            "#5F9EA0" to "Cadet Blue",
            "#00CED1" to "Dark Turquoise",
            "#20B2AA" to "Light Sea Green",
            "#3CB371" to "Medium Sea Green",
            "#2E8B57" to "Sea Green",
            "#228B22" to "Forest Green",
            "#008000" to "Green",
            "#006400" to "Dark Green",
            "#66CDAA" to "Medium Aquamarine",
            "#40E0D0" to "Turquoise",
            "#48D1CC" to "Medium Turquoise",
            "#00BFFF" to "Deep Sky Blue",
            "#1E90FF" to "Dodger Blue",
            "#7B68EE" to "Medium Slate Blue",
            "#87CEFA" to "Light Sky Blue",
            "#87CEEB" to "Sky Blue",
            "#B0E0E6" to "Powder Blue",
            "#ADD8E6" to "Light Blue",
            "#B0C4DE" to "Light Steel Blue",
            "#F0F8FF" to "Alice Blue",
            "#F5F5FF" to "Ghost White",
            "#F0FFFF" to "Azure",
            "#F5FFFA" to "Mint Cream",
            "#FFFFE0" to "Light Yellow",
            "#FFF0F5" to "Lavender Blush",
            "#FFE4E1" to "Misty Rose",
            "#F5DEB3" to "Wheat",
            "#FFEBCD" to "Blanched Almond",
            "#FFB6C1" to "Light Pink",
            "#FFE4B5" to "Moccasin",
            "#FF6347" to "Tomato",
            "#FF7F50" to "Coral",
            "#FF8C00" to "Dark Orange",
            "#FFD700" to "Gold",
            "#FFFF00" to "Yellow",
            "#FFFFE0" to "Light Yellow",
            "#F5F5DC" to "Beige",
            "#E0FFFF" to "Light Cyan",
            "#B0E0E6" to "Powder Blue",
            "#ADD8E6" to "Light Blue",
            "#B0C4DE" to "Light Steel Blue",
            "#4682B4" to "Steel Blue",
            "#6495ED" to "Cornflower Blue",
            "#00BFFF" to "Deep Sky Blue",
            "#1E90FF" to "Dodger Blue",
            "#87CEFA" to "Light Sky Blue",
            "#87CEEB" to "Sky Blue",
            "#00FFFF" to "Cyan",
            "#0000CD" to "Medium Blue",
            "#00008B" to "Dark Blue",
            "#000080" to "Navy",
            "#8B4513" to "Saddle Brown",
            "#A0522D" to "Sienna",
            "#D2691E" to "Chocolate",
            "#CD853F" to "Peru",
            "#BC8F8F" to "Rosy Brown",
            "#F4A460" to "Sandy Brown",
            "#FFD700" to "Gold",
            "#FFFFF0" to "Ivory",
            "#F5DEB3" to "Wheat",
            "#FFE4C4" to "Bisque",
            "#F5F5F5" to "White Smoke",
            "#DCDCDC" to "Gainsboro",
            "#D3D3D3" to "Light Gray",
            "#C0C0C0" to "Silver",
            "#A9A9A9" to "Dark Gray",
            "#808080" to "Gray",
            "#696969" to "Dim Gray",
            "#778899" to "Light Slate Gray",
            "#708090" to "Slate Gray",
            "#2F4F4F" to "Dark Slate Gray",
            "#B0C4DE" to "Light Steel Blue",
            "#B22222" to "Firebrick",
            "#CD5C5C" to "Indian Red",
            "#FF4500" to "Orange Red",
            "#DC143C" to "Crimson",
            "#FF6347" to "Tomato",
            "#FF7F50" to "Coral",
            "#FF8C00" to "Dark Orange",
            "#FFA07A" to "Light Salmon",
            "#FF69B4" to "Hot Pink",
            "#FFB6C1" to "Light Pink",
            "#FF1493" to "Deep Pink",
            "#FFC0CB" to "Pink",
            "#DDA0DD" to "Plum",
            "#DA70D6" to "Orchid",
            "#EE82EE" to "Violet",
            "#9400D3" to "Dark Violet",
            "#8A2BE2" to "Blue Violet",
            "#6A5ACD" to "Slate Blue",
            "#7B68EE" to "Medium Slate Blue",
            "#4169E1" to "Royal Blue",
            "#4682B4" to "Steel Blue",
            "#5F9EA0" to "Cadet Blue",
            "#00CED1" to "Dark Turquoise",
            "#20B2AA" to "Light Sea Green",
            "#3CB371" to "Medium Sea Green",
            "#2E8B57" to "Sea Green",
            "#228B22" to "Forest Green",
            "#008000" to "Green",
            "#006400" to "Dark Green",
            "#66CDAA" to "Medium Aquamarine",
            "#40E0D0" to "Turquoise",
            "#48D1CC" to "Medium Turquoise",
            "#00BFFF" to "Deep Sky Blue",
            "#1E90FF" to "Dodger Blue",
            "#7B68EE" to "Medium Slate Blue",
            "#87CEFA" to "Light Sky Blue",
            "#87CEEB" to "Sky Blue",
            "#B0E0E6" to "Powder Blue",
            "#ADD8E6" to "Light Blue",
            "#B0C4DE" to "Light Steel Blue",
            "#F0F8FF" to "Alice Blue",
            "#F5F5FF" to "Ghost White",
            "#F0FFFF" to "Azure",
            "#F5FFFA" to "Mint Cream",
            "#FFFFE0" to "Light Yellow",
            "#FFF0F5" to "Lavender Blush",
            "#FFE4E1" to "Misty Rose",
            "#F5DEB3" to "Wheat",
            "#FFEBCD" to "Blanched Almond",
            "#FFB6C1" to "Light Pink",
            "#FFE4B5" to "Moccasin",
            "#FF6347" to "Tomato",
            "#FF7F50" to "Coral",
            "#FF8C00" to "Dark Orange",
            "#FFD700" to "Gold",
            "#FFFF00" to "Yellow",
            "#FFFFE0" to "Light Yellow",
            "#F5F5DC" to "Beige",
            "#E0FFFF" to "Light Cyan",
            "#B0E0E6" to "Powder Blue",
            "#ADD8E6" to "Light Blue",
            "#B0C4DE" to "Light Steel Blue",
            "#4682B4" to "Steel Blue",
            "#6495ED" to "Cornflower Blue",
            "#00BFFF" to "Deep Sky Blue",
            "#1E90FF" to "Dodger Blue",
            "#87CEFA" to "Light Sky Blue",
            "#87CEEB" to "Sky Blue",
            "#00FFFF" to "Cyan",
            "#0000CD" to "Medium Blue",
            "#00008B" to "Dark Blue",
            "#000080" to "Navy",
            "#8B4513" to "Saddle Brown",
            "#A0522D" to "Sienna",
            "#D2691E" to "Chocolate",
            "#CD853F" to "Peru",
            "#BC8F8F" to "Rosy Brown",
            "#F4A460" to "Sandy Brown",
            "#FFD700" to "Gold",
            "#FFFFF0" to "Ivory",
            "#F5DEB3" to "Wheat",
            "#FFE4C4" to "Bisque",
            "#F5F5F5" to "White Smoke",
            "#DCDCDC" to "Gainsboro",
            "#D3D3D3" to "Light Gray",
            "#C0C0C0" to "Silver",
            "#A9A9A9" to "Dark Gray",
            "#808080" to "Gray",
            "#696969" to "Dim Gray",
            "#778899" to "Light Slate Gray",
            "#708090" to "Slate Gray",
            "#2F4F4F" to "Dark Slate Gray",
            "#B0C4DE" to "Light Steel Blue",
            "#B22222" to "Firebrick",
            "#CD5C5C" to "Indian Red",
            "#FF4500" to "Orange Red",
            "#DC143C" to "Crimson",
            "#FF6347" to "Tomato",
            "#FF7F50" to "Coral",
        )
        val normalizedHex = hex.uppercase().replace("#", "")

        return if (normalizedHex in colorMap.keys.map { it.replace("#", "") }) {
            colorMap["#$normalizedHex"] ?: "Unknown Color"
        } else {
            "Unknown Color"
        }

    }
}





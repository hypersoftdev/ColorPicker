package com.sample.colorpicker

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.hyeprsoft.ColorPickerView

class MainActivity : AppCompatActivity(), ColorPickerView.OnColorChangeListener {

    private val TAG = "ColorPicker"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        val colorPicker = findViewById<ColorPickerView>(R.id.color_picker)

        colorPicker.setOnColorChangeListener(this)
        colorPicker.setStrokeColor(ContextCompat.getColor(this,R.color.black)) // change stroke color
        colorPicker.setStrokeWidth(20f) // min 10f and max 50f
        colorPicker.setCircleRadius(30f) // min 30f and max 200f
        colorPicker.setChangeStrokeColorRealTime(true)
        colorPicker.getColorName("HexColorCode")

    }

    override fun onColorChanged(color: Int) {
        Log.i(TAG, "onColorChanged: $color")
    }

    override fun onHexColorChanged(hexColor: String) {
        Log.i(TAG, "onColorChanged: $hexColor")
    }
}
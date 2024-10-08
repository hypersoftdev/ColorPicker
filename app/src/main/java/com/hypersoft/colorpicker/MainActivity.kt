package com.hypersoft.colorpicker

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hyeprsoft.picker.ColorPickerView
import com.hypersoft.colorpicker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), ColorPickerView.OnColorChangeListener {

    private val TAG = "ColorPicker"

    // Declare the binding object
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val colorPicker = binding.colorPicker

        colorPicker.setOnColorChangeListener(this)
        colorPicker.setStrokeColor(ContextCompat.getColor(this, R.color.black)) // change stroke color
        colorPicker.setStrokeWidth(20f) // min 10f and max 50f
        colorPicker.setCircleRadius(30f) // min 30f and max 200f
        colorPicker.setChangeStrokeColorRealTime(true)
    }

    override fun onColorChanged(color: Int) {
        Log.i(TAG, "onColorChanged: $color")

        binding.currentColor.setBackgroundColor(color)
        binding.intColor.text="$color"
    }

    override fun onHexColorChanged(hexColor: String) {
        Log.i(TAG, "onColorChanged: $hexColor")

        binding.hexValue.text="$hexColor"
    }
}

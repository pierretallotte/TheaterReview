package com.example.theaterreview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.TOP
        layout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)

        val preferences = getSharedPreferences("settings", MODE_PRIVATE)

        val ttsSwitch = SwitchMaterial(this)
        ttsSwitch.textSize = 20f
        ttsSwitch.setPadding(10,10,10,10)
        ttsSwitch.text = getString(R.string.tts)
        ttsSwitch.isChecked = preferences.getBoolean("tts", false)
        ttsSwitch.setOnCheckedChangeListener { _, isChecked ->
            preferences.edit().putBoolean("tts", isChecked).apply()
        }

        layout.addView(ttsSwitch)

        setContentView(layout)
    }
}
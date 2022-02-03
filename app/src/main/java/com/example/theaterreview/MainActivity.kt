package com.example.theaterreview

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    private fun alert(message: String, title: String) {
        val builder = let { AlertDialog.Builder(it) }
        builder.setMessage(message).setTitle(title)
        val dialog = builder.create()
        dialog.show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.TOP
        layout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = Intent(this, ReviewActivity::class.java)
                val uri = result.data?.data

                if(uri == null) {
                    alert(getString(R.string.no_file), getString(R.string.error))
                } else {
                    intent.putExtra("uri", uri)
                    startActivity(intent)
                }
            }
        }

        val open = Button(this)
        open.textSize = 20f
        open.text = getString(R.string.open_file)
        open.setOnClickListener {
            fun openFilePicker() {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "text/plain"
                }
                resultLauncher.launch(intent)
            }

            openFilePicker()
        }

        layout.addView(open)

        val settingsBtn = Button(this)
        settingsBtn.textSize = 20f
        settingsBtn.text = getString(R.string.settings)
        settingsBtn.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        layout.addView(settingsBtn)

        setContentView(layout)
    }
}
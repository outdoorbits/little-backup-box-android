package org.outdoorbits.lbbscanner

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.slider.Slider

class SetupActivity : BaseActivity() {

	override val layoutResId = R.layout.activity_setup

	private lateinit var slider: Slider
	private lateinit var valueLabel: TextView
	private lateinit var saveBtn: Button

	private val prefs by lazy {
		getSharedPreferences("settings", MODE_PRIVATE)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		supportActionBar?.title = "Setup"
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		valueLabel = findViewById(R.id.textViewValue)
		slider     = findViewById(R.id.sliderTimeout)
		saveBtn    = findViewById(R.id.buttonSave)

		// load stored value or default (500 ms)
		val stored = prefs.getInt("connectTimeout", 200)
		slider.value = stored.coerceIn(100, 1000).toFloat()
		valueLabel.text = "Timeout: $stored ms"

		/* live update while dragging */
		slider.addOnChangeListener { _, value, _ ->
			valueLabel.text = "Timeout: ${value.toInt()} ms"
		}

		saveBtn.setOnClickListener {
			val selected = slider.value.toInt()
			prefs.edit().putInt("connectTimeout", selected).apply()
			Toast.makeText(this, "Connect timeout saved: $selected ms",
						Toast.LENGTH_SHORT).show()
			finish()
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		finish(); return true
	}
}

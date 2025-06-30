package org.outdoorbits.lbbscanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ResourcesActivity : BaseActivity() {
	override val layoutResId = R.layout.activity_resources

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}
}

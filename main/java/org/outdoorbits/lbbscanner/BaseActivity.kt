package org.outdoorbits.lbbscanner

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import androidx.core.view.updatePadding          //  ← für updatePadding

abstract class BaseActivity : AppCompatActivity() {

	/** This must be implemented by all subclasses to define their layout. */
	protected abstract val layoutResId: Int

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()

		super.onCreate(savedInstanceState)

		// Inflate the common base layout with toolbar and content frame
		super.setContentView(R.layout.activity_base)

		// Inflate the child-specific layout into the content frame
		val contentFrame: FrameLayout = findViewById(R.id.contentFrame)
		layoutInflater.inflate(layoutResId, contentFrame, true)

		// Setup the toolbar
		val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
		setSupportActionBar(toolbar)

 		// statusbar-inset as padding on toolbar
		ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
			val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
			v.updatePadding(top = top)
			insets
		}

		ViewCompat.setOnApplyWindowInsetsListener(
			findViewById(android.R.id.content)
		) { v, insets ->
			val safe = insets.getInsets(
				WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
			)
			v.updatePadding(
				left   = safe.left,
				top    = safe.top,
				right  = safe.right,
				bottom = safe.bottom
			)
			WindowInsetsCompat.CONSUMED
		}
	}

	override fun onCreateOptionsMenu(menu: Menu?): Boolean {
		menuInflater.inflate(R.menu.top_app_bar_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.nav_main -> {
				startActivity(Intent(this, LbbScannerActivity::class.java))
				true
			}
			R.id.menu_resources -> {
				startActivity(Intent(this, ResourcesActivity::class.java))
				true
			}
			R.id.menu_wiki -> {
				val intent = Intent(this, WikiActivity::class.java)
				intent.putExtra("assetFile", "_Sidebar.md")
				startActivity(intent)
				true
			}
			else -> super.onOptionsItemSelected(item)
		}
	}
}

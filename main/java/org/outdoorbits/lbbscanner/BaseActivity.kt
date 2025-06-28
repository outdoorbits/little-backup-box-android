package org.outdoorbits.lbbscanner

import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.nav_main -> {
				startActivity(Intent(this, LbbScannerActivity::class.java))
				true
			}
			R.id.menu_resources -> {
				val intent = Intent(this, ResourcesActivity::class.java)
				startActivity(intent)
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

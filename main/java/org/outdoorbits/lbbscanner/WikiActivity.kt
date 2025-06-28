package org.outdoorbits.lbbscanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class WikiActivity : BaseActivity() {
	private lateinit var wikiTextView: TextView
	private var currentWikiFile: String = "_Sidebar.md"

	companion object {
		private const val TAG = "WikiActivity"
		const val EXTRA_WIKI_FILE = "extra_wiki_file"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_wiki)

		// Enable back button in action bar
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		wikiTextView = findViewById(R.id.wikiText)
		wikiTextView.movementMethod = LinkMovementMethod.getInstance()

		// Start file
		currentWikiFile = intent.getStringExtra(EXTRA_WIKI_FILE) ?: "_Sidebar.md"
		loadWikiFile(currentWikiFile)
	}

	private fun loadWikiFile(fileName: String) {
		try {
			currentWikiFile = fileName

			val inputStream = assets.open("wiki/$fileName")
			var markdown = inputStream.bufferedReader().use { it.readText() }.replace(Regex("""\)\s*<br>"""), ")")

			val html = convertMarkdownToHtml(markdown)

			// Title
			val title = fileName
				.replace(".md", "")
				.replace("_", " ")
				.replace("-", " ")
			supportActionBar?.title = title

			wikiTextView.text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT, AssetImageGetter(this), null)

			// Links will use LinkMovementMethod
			wikiTextView.movementMethod = object : LinkMovementMethod() {
				override fun onTouchEvent(widget: TextView, buffer: android.text.Spannable, event: android.view.MotionEvent): Boolean {
					val action = event.action
					if (action == android.view.MotionEvent.ACTION_UP) {
						var x = event.x.toInt()
						var y = event.y.toInt()

						x -= widget.totalPaddingLeft
						y -= widget.totalPaddingTop
						x += widget.scrollX
						y += widget.scrollY

						val layout = widget.layout
						val line = layout.getLineForVertical(y)
						val off = layout.getOffsetForHorizontal(line, x.toFloat())

						val links = buffer.getSpans(off, off, android.text.style.URLSpan::class.java)
						if (links.isNotEmpty()) {
							val url = links[0].url
							handleLinkClick(url)
							return true
						}
					}
					return super.onTouchEvent(widget, buffer, event)
				}
			}

		} catch (e: IOException) {
			Log.e(TAG, "Error loading wiki file: $fileName", e)
			showError("Datei konnte nicht geladen werden: $fileName")
		}
	}

	private fun handleLinkClick(link: String) {
		Log.d(TAG, "Clicked link: $link")
		if (link.endsWith(".md")) {
			loadWikiFile(link)
		} else if (link.startsWith("http://") || link.startsWith("https://")) {
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
		} else {
			Toast.makeText(this, "Unbekannter Link: $link", Toast.LENGTH_SHORT).show()
		}
	}

	private fun convertMarkdownToHtml(markdown: String): String {
		var html = markdown

		// Headers
		html = html.replace(Regex("^#{4}\\s+(.+)$", RegexOption.MULTILINE), "<h4>$1</h4>")
		html = html.replace(Regex("^#{3}\\s+(.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")
		html = html.replace(Regex("^#{2}\\s+(.+)$", RegexOption.MULTILINE), "<h2>$1</h2>")
		html = html.replace(Regex("^#\\s+(.+)$", RegexOption.MULTILINE), "<h1>$1</h1>")

		// Bold text
		html = html.replace(Regex("\\*\\*([^*]+)\\*\\*"), "<b>$1</b>")

		// Replace image links
		html = html.replace(
			Regex("""!\[(?<alt>[^\]]*?)\]\(\s*(?<src>images/[^\)\s]+)\s*(?:"[^"]*")?\)"""),
			"""<img src="file:///android_asset/wiki/${'$'}{src}" alt="${'$'}{alt}" style="max-width:100%;height:auto;" />"""
		)

		// Then normal links
		html = html.replace(
			Regex("""\[(.*?)\]\((.*?)\)"""),
			"<a href=\"$2\">$1</a>"
		)

		// Line breaks
		html = html.replace("\n", "<br/>")

		return html
	}


	private fun showError(message: String) {
		wikiTextView.text = "Fehler: $message"
		Toast.makeText(this, message, Toast.LENGTH_LONG).show()
	}

	override fun onSupportNavigateUp(): Boolean {
		onBackPressed()
		return true
	}

	override fun onBackPressed() {
		if (currentWikiFile == "_Sidebar.md") {
			super.onBackPressed()
		} else {
			loadWikiFile("_Sidebar.md")
		}
	}
}

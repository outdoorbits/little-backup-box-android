package org.outdoorbits.lbbscanner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.MotionEvent
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import java.io.IOException
import java.util.regex.Pattern

class WikiActivity : BaseActivity() {

	override val layoutResId = R.layout.activity_wiki

	private lateinit var wikiTextView: TextView
	private var currentWikiFile: String = "_Sidebar.md"

	companion object {
		private const val TAG = "WikiActivity"
		const val EXTRA_WIKI_FILE = "extra_wiki_file"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		supportActionBar?.setDisplayHomeAsUpEnabled(true)

		wikiTextView = findViewById(R.id.wikiText)
		wikiTextView.movementMethod = LinkMovementMethod.getInstance()

		/* ---- modernes Back-Handling ---- */
		onBackPressedDispatcher.addCallback(
			this,
			object : OnBackPressedCallback(true) {
				override fun handleOnBackPressed() {
					if (currentWikiFile == "_Sidebar.md") finish()
					else loadWikiFile("_Sidebar.md")
				}
			}
		)

		// Startdatei
		currentWikiFile = intent.getStringExtra(EXTRA_WIKI_FILE) ?: "_Sidebar.md"
		loadWikiFile(currentWikiFile)
	}

	/* ---------------- Wiki laden & als HTML zeigen ---------------- */

	private fun loadWikiFile(fileName: String) {
		try {
			currentWikiFile = fileName

			val markdown = assets.open("wiki/$fileName")
				.bufferedReader().readText()
				.replace(Regex("""\)\s*<br>"""), ")")      // dein Work-around

			val html = convertMarkdownToHtml(markdown)

			supportActionBar?.title = fileName
				.removeSuffix(".md")
				.replace('_', ' ')
				.replace('-', ' ')

			wikiTextView.text = Html.fromHtml(
				html,
				Html.FROM_HTML_MODE_COMPACT,
				AssetImageGetter(this),
				null
			)

			/* -------- interner Link-Router -------- */
			wikiTextView.movementMethod = object : LinkMovementMethod() {
				override fun onTouchEvent(
					widget: TextView,
					buffer: android.text.Spannable,
					event: MotionEvent
				): Boolean {
					if (event.action == MotionEvent.ACTION_UP) {
						val x = (event.x - widget.totalPaddingLeft + widget.scrollX).toInt()
						val y = (event.y - widget.totalPaddingTop + widget.scrollY).toInt()
						val layout = widget.layout
						val line = layout.getLineForVertical(y)
						val off = layout.getOffsetForHorizontal(line, x.toFloat())
						val links = buffer.getSpans(off, off, android.text.style.URLSpan::class.java)
						if (links.isNotEmpty()) {
							handleLinkClick(links[0].url)
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

	/* -------- Markdown-→HTML (Schnell-Regex) -------- */

	private fun convertMarkdownToHtml(md: String): String {
		var html = md
		// Header
		html = html.replace(Regex("^#{4}\\s+(.+)$", RegexOption.MULTILINE), "<h4>$1</h4>")
		html = html.replace(Regex("^#{3}\\s+(.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")
		html = html.replace(Regex("^#{2}\\s+(.+)$", RegexOption.MULTILINE), "<h2>$1</h2>")
		html = html.replace(Regex("^#\\s+(.+)$",  RegexOption.MULTILINE), "<h1>$1</h1>")

		// Bold
		html = html.replace(Regex("\\*\\*([^*]+)\\*\\*"), "<b>$1</b>")

		// Images
		val imgPattern = Pattern.compile("""!\[(?<alt>[^\]]*?)\]\(\s*(?<src>images/[^\)\s]+)\s*(?:"[^"]*")?\)""")
		html = imgPattern.matcher(html).replaceAll(
			"""<img src="file:///android_asset/wiki/${'$'}{src}" alt="${'$'}{alt}" style="max-width:100%;height:auto;" />"""
		)

		// Links
		html = html.replace(Regex("""\[(.*?)\]\((.*?)\)"""), "<a href=\"$2\">$1</a>")

		// Zeilenumbrüche
		return html.replace("\n", "<br/>")
	}

	/* -------- Link-Klicks -------- */

	private fun handleLinkClick(link: String) {
		Log.d(TAG, "Clicked link: $link")
		when {
			link.endsWith(".md") -> loadWikiFile(link)
			link.startsWith("http://") || link.startsWith("https://") ->
				startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
			else -> Toast.makeText(this, "Unbekannter Link: $link", Toast.LENGTH_SHORT).show()
		}
	}

	private fun showError(message: String) {
		wikiTextView.text = "Fehler: $message"
		Toast.makeText(this, message, Toast.LENGTH_LONG).show()
	}
}

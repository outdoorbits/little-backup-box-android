package org.outdoorbits.lbbscanner

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.regex.Pattern

class WikiActivity : AppCompatActivity() {

    private lateinit var wikiTextView: TextView

    companion object {
        private const val TAG = "WikiActivity"
        const val EXTRA_WIKI_FILE = "extra_wiki_file"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wiki)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializeViews()

        // Get the file to display from intent or show default
        val wikiFile = intent.getStringExtra(EXTRA_WIKI_FILE) ?: "_Sidebar.md"
        loadWikiFile(wikiFile)
    }

    private fun initializeViews() {
        wikiTextView = findViewById(R.id.wikiText)
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
                        if (url.endsWith(".md")) {
                            // Handle wiki link
                            loadWikiFile(url)
                            return true
                        }
                    }
                }
                return super.onTouchEvent(widget, buffer, event)
            }
        }
    }

    private fun loadWikiFile(fileName: String) {
        try {
            val inputStream = assets.open("wiki/$fileName")
            val markdown = inputStream.bufferedReader().use { it.readText() }

            // Convert markdown to simple HTML
            val html = convertMarkdownToHtml(markdown)

            // Set title based on filename
            val title = fileName.replace(".md", "").replace("_", " ").replace("-", " ")
            supportActionBar?.title = title

            // Display HTML
            wikiTextView.text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)

        } catch (e: IOException) {
            Log.e(TAG, "Error loading wiki file: $fileName", e)
            showError("Datei konnte nicht geladen werden: $fileName")
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

        // Links
        html = html.replace(Regex("\\[([^\\]]+)\\]\\(([^)]+\\.md)\\)"), "<a href=\"$2\">$1</a>")
        html = html.replace(Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)"), "<a href=\"$2\">$1</a>")

        // Images - convert to work with assets
        html = html.replace(
            Regex("!\\[([^\\]]*)\\]\\(images/([^)]+)\\)"),
            "<img src=\"file:///android_asset/wiki/images/$2\" alt=\"$1\" style=\"max-width:100%;height:auto;\" />"
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
}

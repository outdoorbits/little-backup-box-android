package org.outdoorbits.lbbscanner

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.*
import javax.net.ssl.*
import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.content.Intent

class LbbScannerActivity : AppCompatActivity() {
	private lateinit var subnetText: TextView
	private lateinit var findingsText: TextView
	private lateinit var progressBar: ProgressBar
	private lateinit var startButton: Button
	private lateinit var stopButton: Button
	private var scanJob: Job? = null
	private val prefs by lazy {
		getSharedPreferences("lbbscanner_prefs", Context.MODE_PRIVATE)
	}

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
			else -> super.onOptionsItemSelected(item)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_lbbscanner)

		subnetText = findViewById(R.id.subnetText)
		findingsText = findViewById(R.id.findingsText)
		progressBar = findViewById(R.id.progressBar)
		startButton = findViewById(R.id.startButton)
		stopButton = findViewById(R.id.stopButton)

		findingsText.movementMethod = LinkMovementMethod.getInstance()

		val subnet = getLocalSubnet()
		subnetText.text = "Subnet: $subnet.x"

		startButton.setOnClickListener {
			startScan(subnet)
		}

		stopButton.setOnClickListener {
			stopScan()
		}
	}

private fun startScan(subnet: String) {
	stopScan()
	findingsText.text = ""
	progressBar.progress = 0

	val sslContext = getInsecureSslContext()
	val allHostsValid = HostnameVerifier { _, _ -> true }

	val tried = mutableSetOf<Int>()
	val storedHits = getStoredHits(subnet)

	scanJob = CoroutineScope(Dispatchers.IO).launch {
		val allTargets = storedHits + (1..254).filterNot { storedHits.contains(it) }

		for ((index, i) in allTargets.withIndex()) {
			if (!isActive) break
			if (tried.contains(i)) continue
			tried.add(i)

			val ip = "$subnet.$i"

			withContext(Dispatchers.Main) {
				subnetText.text = "Prüfe: $ip"
				progressBar.progress = (index * 100 / allTargets.size)
			}

			try {
				val url = URL("https://$ip/public/app.php")
				val conn = url.openConnection() as HttpsURLConnection
				conn.sslSocketFactory = sslContext.socketFactory
				conn.hostnameVerifier = allHostsValid
				conn.connectTimeout = 500
				conn.readTimeout = 500
				conn.inputStream.bufferedReader().use {
					val content = it.readText()
					if (content.contains("Little Backup Box")) {
						withContext(Dispatchers.Main) {
							findingsText.append("https://$ip/\n\n")
						}
						saveHit(subnet, i)
					}
				}
			} catch (_: Exception) { }
		}

		withContext(Dispatchers.Main) {
			subnetText.text = "Scan abgeschlossen"
		}
	}
}


	private fun stopScan() {
		scanJob?.cancel()
		progressBar.progress = 0
	}

	private fun getLocalSubnet(): String {
		try {
			val interfaces = NetworkInterface.getNetworkInterfaces()
			for (intf in interfaces) {
				val addresses = intf.inetAddresses
				for (addr in addresses) {
					if (!addr.isLoopbackAddress && addr is Inet4Address) {
						val parts = addr.hostAddress.split(".")
						if (parts.size == 4) return parts.subList(0, 3).joinToString(".")
					}
				}
			}
		} catch (e: Exception) {}
		return "192.168.0"
	}

	private fun getInsecureSslContext(): SSLContext {
		val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
			override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
			override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
			override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
		})

		return SSLContext.getInstance("SSL").apply {
			init(null, trustAllCerts, java.security.SecureRandom())
		}
	}

private fun getStoredHits(subnet: String): List<Int> {
	val stored = prefs.getStringSet("hits_$subnet", emptySet()) ?: emptySet()
	return stored.mapNotNull { it.toIntOrNull() }.distinct().sorted()
}

private fun saveHit(subnet: String, byte: Int) {
	val key = "hits_$subnet"
	val current = prefs.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf()
	current.add(byte.toString())
	prefs.edit().putStringSet(key, current).apply()
}
}

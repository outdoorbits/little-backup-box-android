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
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import android.util.Log

class LbbScannerActivity : BaseActivity() {
	override val layoutResId = R.layout.activity_lbbscanner

	private lateinit var subnetText: TextView
	private lateinit var findingsText: TextView
	private lateinit var progressBar: ProgressBar
	private lateinit var startButton: Button
	private lateinit var stopButton: Button
	private var scanJob: Job? = null
	private val prefs by lazy {
		getSharedPreferences("lbbscanner_prefs", Context.MODE_PRIVATE)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		supportActionBar?.title = "Scanner"  // optional title for toolbar

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

	private fun verifyServerCertificate(ip: String): Boolean {
		try {
			val trustManager = CapturingTrustManager()
			val sslContext = SSLContext.getInstance("TLS")
			sslContext.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())

			val url = URL("https://$ip/")
			val conn = url.openConnection() as HttpsURLConnection

			val prefs = getSharedPreferences("settings", MODE_PRIVATE)
			val connectTimeoutMs = prefs.getInt("connectTimeout", 200)

			conn.sslSocketFactory = sslContext.socketFactory
			conn.connectTimeout = connectTimeoutMs
			conn.readTimeout = connectTimeoutMs

			try {
				conn.connect()
			} catch (e: SSLHandshakeException) {
				Log.d("CERT", "Handshake failed (expected), inspecting cert anyway")
			}

			val certs = trustManager.lastServerCerts
			if (!certs.isNullOrEmpty()) {
				Log.d("CERT", "Server certificates received: ${certs.size}")
				for (cert in certs) {
					Log.d("CERT", "SubjectDN: ${cert.subjectX500Principal.name}")

					if (cert.subjectX500Principal.name.contains("little-backup-box", ignoreCase = true)) {
						Log.d("CERT", "Match found!")
						return true
					}
				}
			} else {
				Log.d("CERT", "No server certificates received.")
			}
		} catch (e: Exception) {
			Log.d("CERT", "Expected exception in verifyServerCertificate (used for certificate capture)", e)
		}
		return false
	}

	private fun startScan(subnet: String) {
		stopScan()
		findingsText.text = ""
		progressBar.progress = 0

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
					subnetText.text = "Checking: $ip"
					Log.d("CERT", "IP: $ip")
					progressBar.progress = ((index + 1) * 100 / allTargets.size)
				}

				if (verifyServerCertificate(ip)) {
					withContext(Dispatchers.Main) {
						findingsText.append("https://$ip/\n\n")
					}
					saveHit(subnet, i)
				}
			}

			withContext(Dispatchers.Main) {
				subnetText.text = getString(R.string.scan_completed)
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
						val parts = addr.hostAddress?.split(".") ?: emptyList()
						if (parts.size == 4) return parts.subList(0, 3).joinToString(".")
					}
				}
			}
		} catch (e: Exception) {}
		return "192.168.0"
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

class CapturingTrustManager : X509TrustManager {
	var lastServerCerts: Array<out X509Certificate>? = null

	override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

	override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
		if (chain != null) {
			lastServerCerts = chain
		}
		throw CertificateException("Untrusted on purpose")
	}

	override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
}

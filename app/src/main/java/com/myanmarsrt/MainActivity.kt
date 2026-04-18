package com.myanmarsrt

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.myanmarsrt.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pendingFileCallback: ValueCallback<Array<Uri>>? = null
    private var savedSrtContent: String = ""
    private var savedSrtFilename: String = "subtitle_myanmar.srt"

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingFileCallback ?: return@registerForActivityResult
        pendingFileCallback = null
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                callback.onReceiveValue(arrayOf(uri))
            } else {
                callback.onReceiveValue(null)
            }
        } else {
            callback.onReceiveValue(null)
        }
    }

    // SRT save launcher
    private val saveSrtLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { out ->
                    out.write(("\uFEFF" + savedSrtContent).toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(this, "✅ SRT saved!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        loadApp()

        // Handle file opened from another app (e.g. Files app)
        intent?.data?.let { uri ->
            if (intent.action == Intent.ACTION_VIEW) {
                binding.webView.postDelayed({
                    injectFileUri(uri)
                }, 1500)
            }
        }
    }

    private fun setupWebView() {
        val webView = binding.webView

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(false)
            builtInZoomControls = false
            displayZoomControls = false
        }

        // Force dark mode if system is dark
        if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
            WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, false)
        }

        // JavaScript bridge
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        webView.webChromeClient = object : WebChromeClient() {
            // Handle <input type="file"> pickers from the web app
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                pendingFileCallback?.onReceiveValue(null)
                pendingFileCallback = filePathCallback

                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "audio/*"))
                }
                filePickerLauncher.launch(intent)
                return true
            }

            override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                msg?.let {
                    android.util.Log.d("WebView", "[${it.messageLevel()}] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                }
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                android.util.Log.e("WebView", "Error: ${error?.description} for ${request?.url}")
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                // Allow Gemini API and fonts, block all other external navigation
                return if (url.startsWith("https://generativelanguage.googleapis.com") ||
                           url.startsWith("https://fonts.googleapis.com") ||
                           url.startsWith("https://fonts.gstatic.com") ||
                           url.startsWith("file://") ||
                           url.startsWith("about:")) {
                    false
                } else {
                    android.util.Log.d("WebView", "Blocked navigation to: $url")
                    true
                }
            }
        }
    }

    private fun loadApp() {
        binding.webView.loadUrl("file:///android_asset/www/index.html")
    }

    // Inject a file URI into the web app when opened from another app
    private fun injectFileUri(uri: Uri) {
        val js = "javascript:if(window.injectExternalFile){window.injectExternalFile('$uri');}"
        binding.webView.loadUrl(js)
    }

    inner class AndroidBridge {

        /** Called from JS to read a file (content:// or file://) and return base64 */
        @JavascriptInterface
        fun readFileAsBase64(uriString: String): String {
            return try {
                val uri = Uri.parse(uriString)
                val inputStream: InputStream = contentResolver.openInputStream(uri)
                    ?: return ""
                val bytes = inputStream.readBytes()
                inputStream.close()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (e: Exception) {
                android.util.Log.e("Bridge", "readFileAsBase64 error: ${e.message}")
                ""
            }
        }

        /** Get MIME type of a URI */
        @JavascriptInterface
        fun getMimeType(uriString: String): String {
            val uri = Uri.parse(uriString)
            return contentResolver.getType(uri) ?: "video/mp4"
        }

        /** Get file name of a URI */
        @JavascriptInterface
        fun getFileName(uriString: String): String {
            val uri = Uri.parse(uriString)
            var name = "video"
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) name = cursor.getString(idx)
            }
            return name
        }

        /** Get file size in bytes */
        @JavascriptInterface
        fun getFileSize(uriString: String): Long {
            val uri = Uri.parse(uriString)
            var size = 0L
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst() && idx >= 0) size = cursor.getLong(idx)
            }
            return size
        }

        /** Save SRT content to Downloads folder via system picker */
        @JavascriptInterface
        fun saveSRT(content: String, filename: String) {
            savedSrtContent = content
            savedSrtFilename = if (filename.endsWith(".srt")) filename else "$filename.srt"
            runOnUiThread {
                saveSrtLauncher.launch(savedSrtFilename)
            }
        }

        /** Show a native toast */
        @JavascriptInterface
        fun showToast(message: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }

        /** Returns "true" — lets JS know it's running inside the Android app */
        @JavascriptInterface
        fun isAndroid(): String = "true"

        /** Returns Android SDK version */
        @JavascriptInterface
        fun getSdkVersion(): Int = Build.VERSION.SDK_INT
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}

package com.localflow.pocof4

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.localflow.pocof4.engine.LocalAiPipeline

class MainActivity : AppCompatActivity() {

    private lateinit var pipeline: LocalAiPipeline

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pipeline = LocalAiPipeline.getInstance(applicationContext)
        requestAudioPermissionIfNeeded()

        val webView = WebView(this)
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        setContentView(webView)

        // Loads the full LocalFlow Studio UI bundled in assets/index.html
        webView.loadUrl("file:///android_asset/index.html")
    }

    private fun requestAudioPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                1001
            )
        }
    }

    fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}

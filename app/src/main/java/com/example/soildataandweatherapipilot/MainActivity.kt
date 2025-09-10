package com.example.soildataandweatherapipilot

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupWebView()
        loadLeafletMap()
    }

    private fun setupWebView() {
        webView = findViewById(R.id.webView)

        // Enable JavaScript
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true

        // Add JavaScript interface to receive coordinates from the map
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        // Set WebView client to handle page loading
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // You can perform actions after the page is fully loaded
                // For example, set initial location if needed
                // setMapLocation(28.6139, 77.2090, 12)
            }
        }
    }

    private fun loadLeafletMap() {
        // Load HTML file from assets folder
        webView.loadUrl("file:///android_asset/map.html")
    }

    // JavaScript interface class to receive data from WebView
    inner class WebAppInterface {
        @JavascriptInterface
        fun onLocationClicked(latitude: Double, longitude: Double) {
            // This method will be called from JavaScript when user clicks on the map
            runOnUiThread {
                // Process the coordinates
                handleLocationClick(latitude, longitude)
            }
        }
    }

    private fun handleLocationClick(latitude: Double, longitude: Double) {
        // Handle the received coordinates here
        println("Received coordinates: Lat = $latitude, Lon = $longitude")

        // Update TextViews with the coordinates
        findViewById<TextView>(R.id.txtLat).text = String.format("%.6f", latitude)
        findViewById<TextView>(R.id.txtLon).text = String.format("%.6f", longitude)

        // Optional: Show a brief toast notification
        // Toast.makeText(this, "Location selected", Toast.LENGTH_SHORT).show()

        // 2. Save to SharedPreferences
        // val sharedPref = getSharedPreferences("location_prefs", Context.MODE_PRIVATE)
        // with(sharedPref.edit()) {
        //     putFloat("latitude", latitude.toFloat())
        //     putFloat("longitude", longitude.toFloat())
        //     apply()
        // }

        // 3. Send to API or database
        // sendLocationToAPI(latitude, longitude)

        // 4. Update other UI components
        // updateLocationDisplay(latitude, longitude)
    }

    // Optional: Method to programmatically set map location from Kotlin
    fun setMapLocation(latitude: Double, longitude: Double, zoom: Int = 15) {
        val jsCommand = "javascript:updateMapLocation($latitude, $longitude, $zoom, false);"
        webView.evaluateJavascript(jsCommand, null)
    }

    // Optional: Method to clear all markers from Kotlin
    fun clearMapMarkers() {
        val jsCommand = "javascript:clearMarkers();"
        webView.evaluateJavascript(jsCommand, null)

        // Clear the TextViews as well
        findViewById<TextView>(R.id.txtLat).text = ""
        findViewById<TextView>(R.id.txtLon).text = ""
    }

    // Optional: Get current user location and set on map
    // You'll need location permissions for this
    /*
    private fun setCurrentLocationOnMap() {
        // Implementation for getting current location
        // and calling setMapLocation(lat, lng)
    }
    */
}
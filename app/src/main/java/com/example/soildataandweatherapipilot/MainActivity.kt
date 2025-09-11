package com.example.soildataandweatherapipilot

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.SimpleExpandableListAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    private lateinit var expandableListView: ExpandableListView
    private lateinit var expandableListAdapter: SimpleExpandableListAdapter
    private lateinit var expandableListTitle: List<String>
    private lateinit var expandableListDetail: HashMap<String, List<String>>

    private var lat = 0.00
    private var lon = 0.00

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://rest.isric.org/soilgrids/v2.0/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val apiService = retrofit.create(ApiInterface::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        expandableListView = findViewById(R.id.expandableListView)

        // Load initial data (empty soil classification data)
        expandableListDetail = getInitialData()
        expandableListTitle = ArrayList(expandableListDetail.keys)

        // Initialize adapter
        updateExpandableListView()

        // Click listener for child items
        expandableListView.setOnChildClickListener { _, _, groupPosition, childPosition, _ ->
            val category = expandableListTitle[groupPosition]
            val subcategory = expandableListDetail[category]?.get(childPosition)
            Toast.makeText(this, "Clicked: $subcategory", Toast.LENGTH_SHORT).show()
            true
        }


        findViewById<Button>(R.id.btngetdata).setOnClickListener {
            if (lat != 0.00 && lon != 0.00) {
                lifecycleScope.launch {
                    val soilData = fetchSoilClassification(lat, lon)
                    soilData?.let { data ->
                        runOnUiThread {
                            updateSoilClassificationData(data)
                            Toast.makeText(this@MainActivity, "Soil data updated!", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Failed to fetch soil data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please select a location on the map first", Toast.LENGTH_SHORT).show()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupWebView()
        loadLeafletMap()
    }

    private fun updateSoilClassificationData(soilData: SoilClassification) {
        val soilTypesList = mutableListOf<String>()

        // Extract soil types and their probability values from wrb_class_probability
        for (item in soilData.wrb_class_probability) {
            if (item.size >= 2) {
                val soilType = item[0].toString()
                val probability = item[1].toString()
                soilTypesList.add("$soilType : $probability")
            }
        }

        // Update the expandable list data
        expandableListDetail.clear()
        expandableListDetail["Soil Classification"] = soilTypesList
        expandableListTitle = ArrayList(expandableListDetail.keys)

        // Update the adapter
        updateExpandableListView()

        // Automatically expand the first group to show the data
        expandableListView.expandGroup(0)
    }

    private fun updateExpandableListView() {
        expandableListAdapter = SimpleExpandableListAdapter(
            this,
            expandableListTitle.map { mapOf("CATEGORY" to it) },
            android.R.layout.simple_expandable_list_item_1,
            arrayOf("CATEGORY"),
            intArrayOf(android.R.id.text1),
            expandableListDetail.map { entry ->
                entry.value.map { mapOf("SUBCATEGORY" to it) }
            },
            android.R.layout.simple_list_item_1,
            arrayOf("SUBCATEGORY"),
            intArrayOf(android.R.id.text1)
        )
        expandableListView.setAdapter(expandableListAdapter)
    }

    suspend fun fetchSoilClassification(latitude: Double, longitude: Double): SoilClassification? {
        return try {
            val response = apiService.getSoilClassification(
                longitude = longitude,
                latitude = latitude,
                numberClasses = 30
            )

            if (response.isSuccessful) {
                response.body()
            } else {
                println("API Error: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            println("Network Error: ${e.message}")
            null
        }
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
        lat = String.format("%.6f", latitude).toDouble()
        findViewById<TextView>(R.id.txtLon).text = String.format("%.6f", longitude)
        lon = String.format("%.6f", longitude).toDouble()

        // Clear previous soil data when new location is selected
        expandableListDetail.clear()
        expandableListDetail["Soil Classification"] = listOf("Select 'Get Data' to fetch soil information")
        updateExpandableListView()
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

        // Reset coordinates
        lat = 0.00
        lon = 0.00

        // Reset expandable list to initial state
        expandableListDetail = getInitialData()
        expandableListTitle = ArrayList(expandableListDetail.keys)
        updateExpandableListView()
    }

    private fun getInitialData(): HashMap<String, List<String>> {
        val listData = HashMap<String, List<String>>()
        listData["Soil Classification"] = listOf("Please select a location on the map and click 'Get Data'")
        return listData
    }
}
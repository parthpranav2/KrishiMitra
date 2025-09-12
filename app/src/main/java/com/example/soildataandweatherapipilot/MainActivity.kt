package com.example.soildataandweatherapipilot

import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.CheckBox
import android.widget.ExpandableListView
import android.widget.ImageView
import android.widget.LinearLayout
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

    val soilTypesList = mutableListOf<String>()

    // Enhanced data structure to store detailed soil property information
    private val soilPropertyData = mutableMapOf<String, MutableList<String>>()

    // Data structure to hold raw soil properties response for future processing
    private var latestSoilPropertiesResponse: SoilProperties? = null

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

        // Initialize soil property data structure
        initializeSoilPropertyData()

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

        setupDropdownHeaders()
        setupCheckboxListeners()

        findViewById<Button>(R.id.btngetdata).setOnClickListener {
            if (lat != 0.00 && lon != 0.00) {
                // Validate that at least one property is selected
                if (buildPropertiesArray().isEmpty()) {
                    Toast.makeText(this, "Please select at least one soil property", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Validate that at least one depth is selected
                if (buildDepthsArray().isEmpty()) {
                    Toast.makeText(this, "Please select at least one depth range", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Validate that at least one value is selected
                if (buildValuesArray().isEmpty()) {
                    Toast.makeText(this, "Please select at least one value type", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

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

                    val soilData1 = fetchSoilProperties(lat, lon)
                    soilData1?.let { data ->
                        runOnUiThread {
                            // Store the response for detailed processing
                            latestSoilPropertiesResponse = data
                            processSoilPropertiesData(data)
                            Toast.makeText(this@MainActivity, "Soil properties updated!", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Failed to fetch soil properties", Toast.LENGTH_SHORT).show()
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

    private fun setupDropdownHeaders() {
        val header = findViewById<LinearLayout>(R.id.headerLayout)
        val checkboxGroup = findViewById<LinearLayout>(R.id.soilPropcheckboxGroup)
        val arrowIcon = findViewById<ImageView>(R.id.arrowIcon)

        header.setOnClickListener {
            if (checkboxGroup.visibility == View.GONE) {
                checkboxGroup.visibility = View.VISIBLE
                arrowIcon.setImageResource(R.drawable.windup)
            } else {
                checkboxGroup.visibility = View.GONE
                arrowIcon.setImageResource(R.drawable.dropdown)
            }

            // Update expandable list based on selected checkboxes
            updateExpandableListBasedOnSelection()
        }

        val header2 = findViewById<LinearLayout>(R.id.headerLayout2)
        val checkboxGroup2 = findViewById<LinearLayout>(R.id.dOIcheckboxGroup)
        val arrowIcon2 = findViewById<ImageView>(R.id.arrowIcon2)

        header2.setOnClickListener {
            if (checkboxGroup2.visibility == View.GONE) {
                checkboxGroup2.visibility = View.VISIBLE
                arrowIcon2.setImageResource(R.drawable.windup)
            } else {
                checkboxGroup2.visibility = View.GONE
                arrowIcon2.setImageResource(R.drawable.dropdown)
            }

            // Update expandable list based on selected checkboxes
            updateExpandableListBasedOnSelection()
        }

        val header3 = findViewById<LinearLayout>(R.id.headerLayout3)
        val checkboxGroup3 = findViewById<LinearLayout>(R.id.valuecheckboxGroup)
        val arrowIcon3 = findViewById<ImageView>(R.id.arrowIcon3)

        header3.setOnClickListener {
            if (checkboxGroup3.visibility == View.GONE) {
                checkboxGroup3.visibility = View.VISIBLE
                arrowIcon3.setImageResource(R.drawable.windup)
            } else {
                checkboxGroup3.visibility = View.GONE
                arrowIcon3.setImageResource(R.drawable.dropdown)
            }

            // Update expandable list based on selected checkboxes
            updateExpandableListBasedOnSelection()
        }
    }

    private fun setupCheckboxListeners() {
        // Property checkboxes
        val propertyCheckboxes = listOf(
            R.id.bdod, R.id.cec, R.id.cfvo, R.id.clay, R.id.nitrogen,
            R.id.ocd, R.id.ocs, R.id.phh2o, R.id.sand, R.id.silt,
            R.id.soc, R.id.wv0010, R.id.wv0033, R.id.wv1500
        )

        propertyCheckboxes.forEach { checkboxId ->
            findViewById<CheckBox>(checkboxId).setOnCheckedChangeListener { _, _ ->
                updateExpandableListBasedOnSelection()
                // Refresh data if location is selected and we have previous response
                if (lat != 0.00 && lon != 0.00 && latestSoilPropertiesResponse != null) {
                    refreshDataBasedOnAllSelections()
                }
            }
        }

        // Depth checkboxes
        val depthCheckboxes = listOf(
            R.id.dep0_5, R.id.dep5_15, R.id.dep15_30,
            R.id.dep30_60, R.id.dep60_100, R.id.dep100_200
        )

        depthCheckboxes.forEach { checkboxId ->
            findViewById<CheckBox>(checkboxId).setOnCheckedChangeListener { _, _ ->
                onDepthSelectionChanged()
            }
        }

        // Value checkboxes
        val valueCheckboxes = listOf(
            R.id.q_0_5, R.id.q_0_05, R.id.q_0_95, R.id.mean, R.id.uncertainty
        )

        valueCheckboxes.forEach { checkboxId ->
            findViewById<CheckBox>(checkboxId).setOnCheckedChangeListener { _, _ ->
                onValueSelectionChanged()
            }
        }
    }

    private fun initializeSoilPropertyData() {
        val propertyNames = listOf(
            "bdod", "cec", "cfvo", "clay", "nitrogen", "ocd",
            "ocs", "phh2o", "sand", "silt", "soc", "wv0010", "wv0033", "wv1500"
        )

        propertyNames.forEach { property ->
            soilPropertyData[property] = mutableListOf()
        }
    }

    private fun processSoilPropertiesData(soilData: SoilProperties) {
        // Clear previous property data
        soilPropertyData.values.forEach { it.clear() }

        // Get selected depths and values for filtering
        val selectedDepths = buildDepthsArray().toSet()
        val selectedValues = buildValuesArray().toSet()

        // Process each layer in the soil properties response
        soilData.properties?.layers?.forEach { layer ->
            val propertyName = layer.name
            val propertyDetails = mutableListOf<String>()

            // Extract depth-specific data
            layer.depths?.forEach { depthData ->
                val depthLabel = depthData.label // e.g., "0-5cm", "5-15cm"

                // Only process if this depth is selected by the user
                if (selectedDepths.contains(depthLabel)) {
                    // Extract values for each selected statistic
                    depthData.values?.let { values ->
                        // Process each selected value type
                        selectedValues.forEach { valueType ->
                            when (valueType) {
                                "Q0.5" -> {
                                    values.q05?.let { q5Value ->
                                        propertyDetails.add("$depthLabel, Q0.5: $q5Value")
                                    }
                                }
                                "Q0.05" -> {
                                    values.q005?.let { q05Value ->
                                        propertyDetails.add("$depthLabel, Q0.05: $q05Value")
                                    }
                                }
                                "Q0.95" -> {
                                    values.q095?.let { q95Value ->
                                        propertyDetails.add("$depthLabel, Q0.95: $q95Value")
                                    }
                                }
                                "mean" -> {
                                    values.mean?.let { meanValue ->
                                        propertyDetails.add("$depthLabel, mean: $meanValue")
                                    }
                                }
                                "uncertainty" -> {
                                    values.uncertainty?.let { uncertaintyValue ->
                                        propertyDetails.add("$depthLabel, uncertainty: $uncertaintyValue")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Store the processed data
            if (propertyDetails.isNotEmpty() && soilPropertyData.containsKey(propertyName)) {
                soilPropertyData[propertyName!!] = propertyDetails
            }
        }

        // Update the expandable list view with new data
        updateExpandableListBasedOnSelection()
    }

    private fun updateExpandableListBasedOnSelection() {
        expandableListDetail.clear()

        // Map checkbox IDs to property names and display names
        val propertyMapping = mapOf(
            R.id.bdod to Pair("bdod", "Bulk density"),
            R.id.cec to Pair("cec", "Carbon Exchange Capacity"),
            R.id.cfvo to Pair("cfvo", "Coarse Fragment"),
            R.id.clay to Pair("clay", "Clay Content"),
            R.id.nitrogen to Pair("nitrogen", "Nitrogen"),
            R.id.ocd to Pair("ocd", "Organic Carbon Density"),
            R.id.ocs to Pair("ocs", "Organic Carbon Stock"),
            R.id.phh2o to Pair("phh2o", "pH water"),
            R.id.sand to Pair("sand", "Sand Content"),
            R.id.silt to Pair("silt", "Silt Content"),
            R.id.soc to Pair("soc", "Soil Organic Carbon"),
            R.id.wv0010 to Pair("wv0010", "Vol. water content at -10 kPa"),
            R.id.wv0033 to Pair("wv0033", "Vol. water content at -33 kPa"),
            R.id.wv1500 to Pair("wv1500", "Vol. water content at -1500 kPa")
        )

        // Add selected properties to expandable list
        propertyMapping.forEach { (checkboxId, propertyInfo) ->
            if (findViewById<CheckBox>(checkboxId).isChecked) {
                val (propertyKey, displayName) = propertyInfo
                val propertyData = soilPropertyData[propertyKey]

                if (propertyData != null && propertyData.isNotEmpty()) {
                    expandableListDetail[displayName] = propertyData
                } else {
                    val defaultMessage = if (lat != 0.00 && lon != 0.00) {
                        "Select 'Get Data' to fetch soil information"
                    } else {
                        "Please select a location on the map and click 'Get Data'"
                    }
                    expandableListDetail[displayName] = listOf(defaultMessage)
                }
            }
        }

        // Always add soil classification
        expandableListDetail["Soil Classification"] = if (soilTypesList.isNotEmpty()) {
            soilTypesList
        } else {
            val defaultMessage = if (lat != 0.00 && lon != 0.00) {
                listOf("Select 'Get Data' to fetch soil information")
            } else {
                listOf("Please select a location on the map and click 'Get Data'")
            }
            defaultMessage
        }

        expandableListTitle = ArrayList(expandableListDetail.keys)

        // Update the adapter
        updateExpandableListView()

        // Automatically expand the first group to show the data
        if (expandableListDetail.isNotEmpty()) {
            expandableListView.expandGroup(0)
        }
    }

    private fun updateSoilClassificationData(soilData: SoilClassification) {
        // Clear previous soil types data
        soilTypesList.clear()

        // Extract soil types and their probability values from wrb_class_probability
        for (item in soilData.wrb_class_probability) {
            if (item.size >= 2) {
                val soilType = item[0].toString()
                val probability = item[1].toString()
                soilTypesList.add("$soilType : $probability %")
            }
        }

        // Update the expandable list view
        updateExpandableListBasedOnSelection()
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

    fun buildPropertiesArray(): Array<String> {
        val checkboxToProperty = mapOf(
            R.id.bdod to "bdod",
            R.id.cec to "cec",
            R.id.cfvo to "cfvo",
            R.id.clay to "clay",
            R.id.nitrogen to "nitrogen",
            R.id.ocd to "ocd",
            R.id.ocs to "ocs",
            R.id.phh2o to "phh2o",
            R.id.sand to "sand",
            R.id.silt to "silt",
            R.id.soc to "soc",
            R.id.wv0010 to "wv0010",
            R.id.wv0033 to "wv0033",
            R.id.wv1500 to "wv1500"
        )

        return checkboxToProperty.filter { (checkboxId, _) ->
            findViewById<CheckBox>(checkboxId).isChecked
        }.values.toTypedArray()
    }

    fun buildDepthsArray(): Array<String> {
        val checkboxToDepth = mapOf(
            R.id.dep0_5 to "0-5cm",
            R.id.dep5_15 to "5-15cm",
            R.id.dep15_30 to "15-30cm",
            R.id.dep30_60 to "30-60cm",
            R.id.dep60_100 to "60-100cm",
            R.id.dep100_200 to "100-200cm"
        )

        return checkboxToDepth.filter { (checkboxId, _) ->
            findViewById<CheckBox>(checkboxId).isChecked
        }.values.toTypedArray()
    }

    fun buildValuesArray(): Array<String> {
        val checkboxToValue = mapOf(
            R.id.q_0_5 to "Q0.5",
            R.id.q_0_05 to "Q0.05",
            R.id.q_0_95 to "Q0.95",
            R.id.mean to "mean",
            R.id.uncertainty to "uncertainty"
        )

        return checkboxToValue.filter { (checkboxId, _) ->
            findViewById<CheckBox>(checkboxId).isChecked
        }.values.toTypedArray()
    }

    suspend fun fetchSoilProperties(latitude: Double, longitude: Double): SoilProperties? {
        return try {
            val response = apiService.getSoilProperties(
                longitude = longitude,
                latitude = latitude,
                properties = buildPropertiesArray(),
                depths = buildDepthsArray(),
                values = buildValuesArray()
            )

            if (response.isSuccessful) {
                val requestUrl = response.raw().request.url.toString()
                Log.d("fetchSoilProperties", "Request URL: $requestUrl")

                val soilData = response.body()

                // Log the received properties
                val receivedProperties = soilData?.properties?.layers?.map { it.name } ?: emptyList()
                Log.d("fetchSoilProperties", "Properties received: $receivedProperties")
                Log.d("fetchSoilProperties", "Total properties: ${receivedProperties.size}")

                response.body()
            } else {
                Log.d("fetchSoilProperties", "API Error: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("fetchSoilProperties", "Network Error: ${e.message}")
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
        soilTypesList.clear()
        soilPropertyData.values.forEach { it.clear() }
        latestSoilPropertiesResponse = null

        // Update expandable list with location selected message
        updateExpandableListBasedOnSelection()
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

        // Clear all soil data
        soilTypesList.clear()
        soilPropertyData.values.forEach { it.clear() }
        latestSoilPropertiesResponse = null

        // Reset expandable list to initial state
        expandableListDetail = getInitialData()
        expandableListTitle = ArrayList(expandableListDetail.keys)
        updateExpandableListView()
    }

    private fun getInitialData(): HashMap<String, List<String>> {
        val listData = HashMap<String, List<String>>()
        val defaultMessage = listOf("Please select a location on the map and click 'Get Data'")

        // Add default messages for all possible properties
        if (findViewById<CheckBox>(R.id.bdod).isChecked) {
            listData["Bulk density"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.cec).isChecked) {
            listData["Carbon Exchange Capacity"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.cfvo).isChecked) {
            listData["Coarse Fragment"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.clay).isChecked) {
            listData["Clay Content"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.nitrogen).isChecked) {
            listData["Nitrogen"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.ocd).isChecked) {
            listData["Organic Carbon Density"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.ocs).isChecked) {
            listData["Organic Carbon Stock"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.phh2o).isChecked) {
            listData["pH water"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.sand).isChecked) {
            listData["Sand Content"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.silt).isChecked) {
            listData["Silt Content"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.soc).isChecked) {
            listData["Soil Organic Carbon"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.wv0010).isChecked) {
            listData["Vol. water content at -10 kPa"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.wv0033).isChecked) {
            listData["Vol. water content at -33 kPa"] = defaultMessage
        }
        if (findViewById<CheckBox>(R.id.wv1500).isChecked) {
            listData["Vol. water content at -1500 kPa"] = defaultMessage
        }

        listData["Soil Classification"] = defaultMessage
        return listData
    }

    /**
     * Method to handle depth selection changes
     */
    private fun onDepthSelectionChanged() {
        // Re-process the latest soil properties response with new depth selection
        latestSoilPropertiesResponse?.let { response ->
            processSoilPropertiesData(response)
        }
    }

    /**
     * Method to handle value selection changes
     */
    private fun onValueSelectionChanged() {
        // Re-process the latest soil properties response with new value selection
        latestSoilPropertiesResponse?.let { response ->
            processSoilPropertiesData(response)
        }
    }

    /**
     * Method to refresh data when any selection (properties, depths, or values) changes
     */
    private fun refreshDataBasedOnAllSelections() {
        if (lat != 0.00 && lon != 0.00) {
            lifecycleScope.launch {
                val soilData = fetchSoilProperties(lat, lon)
                soilData?.let { data ->
                    runOnUiThread {
                        latestSoilPropertiesResponse = data
                        processSoilPropertiesData(data)
                    }
                }
            }
        }
    }
}
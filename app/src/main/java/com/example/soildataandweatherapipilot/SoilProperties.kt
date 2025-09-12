package com.example.soildataandweatherapipilot

import com.google.gson.annotations.SerializedName

data class SoilProperties(
    val type: String?,
    val geometry: Geometry?,
    val properties: Properties?,
    @SerializedName("query_time_s") val queryTimeS: Double?
)
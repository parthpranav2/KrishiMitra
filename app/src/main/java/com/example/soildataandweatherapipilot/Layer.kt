package com.example.soildataandweatherapipilot

import com.google.gson.annotations.SerializedName

data class Layer(
    val name: String?,
    @SerializedName("unit_measure") val unitMeasure: UnitMeasure?,
    val depths: List<Depth>?
)
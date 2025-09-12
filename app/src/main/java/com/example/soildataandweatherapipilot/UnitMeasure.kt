package com.example.soildataandweatherapipilot

import com.google.gson.annotations.SerializedName

data class UnitMeasure(
    @SerializedName("d_factor") val dFactor: Int?,
    @SerializedName("mapped_units") val mappedUnits: String?,
    @SerializedName("target_units") val targetUnits: String?,
    @SerializedName("uncertainty_unit") val uncertaintyUnit: String?
)
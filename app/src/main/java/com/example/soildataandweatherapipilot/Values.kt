package com.example.soildataandweatherapipilot

import com.google.gson.annotations.SerializedName

data class Values(
    @SerializedName("Q0.05") val q005: Int?,
    @SerializedName("Q0.5") val q05: Int?,
    @SerializedName("Q0.95") val q095: Int?,
    val mean: Int?,
    val uncertainty: Int?
)
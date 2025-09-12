package com.example.soildataandweatherapipilot

import com.google.gson.annotations.SerializedName

data class Range(
    @SerializedName("top_depth") val topDepth: Int?,
    @SerializedName("bottom_depth") val bottomDepth: Int?,
    @SerializedName("unit_depth") val unitDepth: String?
)
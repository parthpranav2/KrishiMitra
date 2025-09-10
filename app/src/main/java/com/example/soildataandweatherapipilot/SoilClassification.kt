package com.example.soildataandweatherapipilot

data class SoilClassification(
    val coordinates: List<Double>,
    val query_time_s: Double,
    val type: String,
    val wrb_class_name: String,
    val wrb_class_probability: List<List<Any>>,
    val wrb_class_value: Int
)
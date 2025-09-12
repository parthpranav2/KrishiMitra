package com.example.soildataandweatherapipilot

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query


// Retrofit interface for the API
// Retrofit interface for both APIs
interface ApiInterface {
    @GET("classification/query")
    suspend fun getSoilClassification(
        @Query("lon") longitude: Double,
        @Query("lat") latitude: Double,
        @Query("number_classes") numberClasses: Int = 30
    ): Response<SoilClassification>

    @GET("properties/query")
    suspend fun getSoilProperties(
        @Query("lon") longitude: Double,
        @Query("lat") latitude: Double,
        @Query("property", encoded = true) properties: Array<String>,
        @Query("depth", encoded = true) depths: Array<String>,
        @Query("value", encoded = true) values: Array<String>
    ): Response<SoilProperties>
}
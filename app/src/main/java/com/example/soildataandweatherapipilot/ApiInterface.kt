package com.example.soildataandweatherapipilot

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query


// Retrofit interface for the API
interface ApiInterface {
    @GET("classification/query")
    suspend fun getSoilClassification(
        @Query("lon") longitude: Double,
        @Query("lat") latitude: Double,
        @Query("number_classes") numberClasses: Int = 30
    ): Response<SoilClassification>
}
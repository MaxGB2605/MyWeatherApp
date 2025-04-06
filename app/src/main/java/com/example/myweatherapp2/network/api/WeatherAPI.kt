package com.example.myweatherapp2.network.api

import com.example.myweatherapp2.data.RemoteLocation
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherAPI {


    companion object {
        const val BASE_URL = "https://api.weatherapi.com/v1"
        const val API_KEY = "f7ce63eeaaa248079d7143947250604"
    }

    @GET("search.json")
    suspend fun searchLocation(
        @Query("key") key: String = API_KEY,
        @Query("q") query: String,
    ): Response<List<RemoteLocation>>

}
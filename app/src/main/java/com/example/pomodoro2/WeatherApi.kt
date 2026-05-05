package com.example.pomodoro2

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Cấu trúc dữ liệu trả về từ OpenWeatherMap
data class WeatherResponse(
    val weather: List<WeatherCondition>,
    val main: MainTemp
)

data class WeatherCondition(
    val main: String, // Ví dụ: "Rain", "Clouds", "Clear"
    val description: String
)

data class MainTemp(
    val temp: Double
)

interface WeatherApiService {
    @GET("data/2.5/weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): WeatherResponse
}

object WeatherApi {
    private const val BASE_URL = "https://api.openweathermap.org/"

    val retrofitService: WeatherApiService by lazy {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .build()
            .create(WeatherApiService::class.java)
    }
}

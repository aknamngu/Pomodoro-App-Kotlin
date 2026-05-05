package com.example.pomodoro2

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class Quote(
    val q: String, // The quote
    val a: String, // The author
    val h: String  // HTML version
)

interface QuotesApiService {
    @GET("api/random")
    suspend fun getRandomQuote(): List<Quote>
}

object QuotesApi {
    private const val BASE_URL = "https://zenquotes.io/"

    val retrofitService: QuotesApiService by lazy {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .build()
            .create(QuotesApiService::class.java)
    }
}

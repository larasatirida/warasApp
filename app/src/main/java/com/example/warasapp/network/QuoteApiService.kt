package com.example.warasapp.network

import retrofit2.http.GET

interface QuoteApiService {
        @GET("quotes/random")
        suspend fun getRandomQuote(): QuoteResponse
}

package com.example.cricketwatchdog

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class MatchData(
    val data: List<MatchInfo>
)

data class MatchInfo(
    val id: String,
    val name: String,
    val status: String,
    val score: List<ScoreInfo>?
)

data class ScoreInfo(
    val r: Int, // runs
    val w: Int, // wickets
    val o: Double, // overs
    val inning: String
)

enum class EventType {
    NONE, FOUR, SIX, WICKET, DRS
}

interface MatchAPI {
    @GET("currentMatches")
    suspend fun getScore(@Query("apikey") apiKey: String): MatchData

    companion object {
        private const val BASE_URL = "https://api.cricapi.com/v1/"

        fun create(): MatchAPI {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MatchAPI::class.java)
        }
    }
}

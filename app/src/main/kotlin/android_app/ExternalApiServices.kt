package android_app

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// OpenWeatherMap: lấy thời tiết hiện tại (model WeatherResponse nằm trong DataModels.kt)
interface WeatherApiService {

    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "vi"
    ): WeatherResponse

    companion object {
        private const val BASE_URL = "https://api.openweathermap.org/"

        fun create(): WeatherApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WeatherApiService::class.java)
        }
    }
}
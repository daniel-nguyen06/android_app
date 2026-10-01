package android_app

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface PlacesApiService {

    // 1. Tìm theo danh mục Places v2
    @GET("v2/places")
    suspend fun searchPlaces(
        @Query("categories") categories: String,
        @Query("name") name: String? = null,
        @Query("bias") bias: String? = null,
        @Query("limit") limit: Int = 15,
        @Query("apiKey") apiKey: String
    ): GeoapifyResponse

    // 2. Tìm kiếm tự do toàn văn (Fuzzy / Text search) - nhận diện "Cộng Coffee", "Highlands", v.v.
    @GET("v1/geocode/search")
    suspend fun geocodeSearch(
        @Query("text") text: String,
        @Query("bias") bias: String? = null,
        @Query("limit") limit: Int = 15,
        @Query("apiKey") apiKey: String
    ): GeoapifyResponse

    companion object {
        private const val BASE_URL = "https://api.geoapify.com/"

        fun create(): PlacesApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlacesApiService::class.java)
        }
    }
}
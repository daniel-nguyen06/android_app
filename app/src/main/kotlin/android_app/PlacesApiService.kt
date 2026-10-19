package android_app

<<<<<<< HEAD
import retrofit2.Call
=======
import com.travel.android_app.BuildConfig
import okhttp3.OkHttpClient
>>>>>>> origin/fix/database
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
<<<<<<< HEAD

interface PlacesApiService {
    // Gọi Text Search của Google Places API
    @GET("maps/api/place/textsearch/json")
    fun searchPlaces(
        @Query("query") query: String,
        @Query("type") type: String? = null,       // restaurant, lodging, tourist_attraction, cafe...
        @Query("key") apiKey: String
    ): Call<PlacesSearchResponse>

    companion object {
        private const val BASE_URL = "https://maps.googleapis.com/"

        fun create(): PlacesApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(PlacesApiService::class.java)
=======
import java.util.concurrent.TimeUnit

// --- Các Data Model để hứng dữ liệu trả về từ Google Maps API ---
data class GooglePlacesResponse(
    val results: List<GooglePlaceResult>,
    val status: String
)

data class GooglePlaceResult(
    val place_id: String,
    val name: String,
    val formatted_address: String?,
    val rating: Double?,
    val price_level: Int?,
    val geometry: GoogleGeometry,
    val types: List<String>?
)

data class GoogleGeometry(
    val location: GoogleLocation
)

data class GoogleLocation(
    val lat: Double,
    val lng: Double
)

interface PlacesApiService {

    // Dùng Google Places Text Search API
    @GET("place/textsearch/json")
    suspend fun searchPlacesWithGoogle(
        @Query("query") query: String,
        @Query("location") location: String? = null,
        @Query("radius") radius: Int? = null,
        @Query("language") language: String = "vi"
    ): GooglePlacesResponse

    companion object {
        private const val BASE_URL = "https://maps.googleapis.com/maps/api/"

        fun create(): PlacesApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val originalHttpUrl = original.url

                    val url = originalHttpUrl.newBuilder()
                        .addQueryParameter("key", BuildConfig.MAPS_API_KEY)
                        .build()

                    val requestBuilder = original.newBuilder().url(url)
                    val request = requestBuilder.build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlacesApiService::class.java)
>>>>>>> origin/fix/database
        }
    }
}
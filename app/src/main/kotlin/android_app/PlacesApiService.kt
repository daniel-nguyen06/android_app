package android_app

import com.google.gson.annotations.SerializedName
import com.travel.android_app.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ---------- Request ----------
data class TextSearchRequest(
    val textQuery: String,
    val languageCode: String? = null,
    val includedType: String? = null,
    val locationBias: LocationBias? = null
)

data class LocationBias(val circle: Circle)
data class Circle(val center: LatLngLiteral, val radius: Double)
data class LatLngLiteral(val latitude: Double, val longitude: Double)

// ---------- Response (Places API New) ----------
data class TextSearchResponse(
    val places: List<NewPlace>? = emptyList()
)

data class NewPlace(
    val id: String? = null,
    val displayName: LocalizedText? = null,
    val formattedAddress: String? = null,
    val rating: Double? = null,
    val userRatingCount: Int? = null,
    /** Dạng chuỗi: PRICE_LEVEL_INEXPENSIVE, PRICE_LEVEL_MODERATE... */
    val priceLevel: String? = null,
    val location: LatLngLiteral? = null,
    val types: List<String>? = null,
    val currentOpeningHours: NewOpeningHours? = null,
    val regularOpeningHours: NewOpeningHours? = null,
    val photos: List<NewPhoto>? = null
)

data class LocalizedText(
    val text: String? = null,
    @SerializedName("languageCode") val languageCode: String? = null
)

data class NewOpeningHours(
    val openNow: Boolean? = null,
    val weekdayDescriptions: List<String>? = null
)

data class NewPhoto(
    /** Dạng "places/{placeId}/photos/{photoRef}" */
    val name: String? = null,
    val widthPx: Int? = null,
    val heightPx: Int? = null
)

interface PlacesApiService {

    @POST("v1/places:searchText")
    suspend fun searchText(
        @Body body: TextSearchRequest,
        @Header("X-Goog-FieldMask") fieldMask: String = DEFAULT_FIELD_MASK
    ): TextSearchResponse

    companion object {
        private const val BASE_URL = "https://places.googleapis.com/"

        const val DEFAULT_FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.rating," +
                    "places.userRatingCount,places.priceLevel,places.location,places.types," +
                    "places.currentOpeningHours,places.regularOpeningHours,places.photos"

        fun photoUrl(photoName: String, maxWidthPx: Int = 800): String =
            "${BASE_URL}v1/$photoName/media?maxWidthPx=$maxWidthPx&key=${BuildConfig.MAPS_API_KEY}"

        fun create(): PlacesApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("X-Goog-Api-Key", BuildConfig.MAPS_API_KEY)
                        .build()
                    chain.proceed(request)
                }
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
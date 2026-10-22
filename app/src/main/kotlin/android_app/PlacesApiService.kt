package android_app

import com.google.gson.annotations.SerializedName
import com.travel.android_app.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ---------------- Geoapify ----------------
data class GeoapifyResponse(
    @SerializedName("features") val features: List<GeoapifyFeature>? = emptyList()
)

data class GeoapifyFeature(
    @SerializedName("properties") val properties: GeoapifyProperties? = null,
    /** [lonMin, latMin, lonMax, latMax] – có ở kết quả geocoding */
    @SerializedName("bbox") val bbox: List<Double>? = null
)

data class GeoapifyProperties(
    @SerializedName("place_id") val placeId: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("result_type") val resultType: String? = null,
    @SerializedName("formatted") val formatted: String? = null,
    @SerializedName("address_line1") val addressLine1: String? = null,
    @SerializedName("address_line2") val addressLine2: String? = null,
    @SerializedName("categories") val categories: List<String>? = emptyList(),
    @SerializedName("opening_hours") val openingHours: String? = null,
    @SerializedName("website") val website: String? = null,
    @SerializedName("datasource") val datasource: GeoapifyDatasource? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("lon") val lon: Double? = null
)

data class GeoapifyDatasource(
    @SerializedName("raw") val raw: Map<String, Any?>? = null
)

interface PlacesApiService {

    @GET("v2/places")
    suspend fun searchPlaces(
        @Query("categories") categories: String,
        @Query("filter") filter: String? = null,
        @Query("bias") bias: String? = null,
        @Query("name") name: String? = null,
        @Query("lang") lang: String? = null,
        @Query("limit") limit: Int = 30
    ): GeoapifyResponse

    @GET("v1/geocode/search")
    suspend fun geocode(
        @Query("text") text: String,
        @Query("lang") lang: String? = null,
        @Query("limit") limit: Int = 1
    ): GeoapifyResponse

    companion object {
        private const val BASE_URL = "https://api.geoapify.com/"

        fun create(): PlacesApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    val original = chain.request()
                    val url = original.url.newBuilder()
                        .addQueryParameter("apiKey", BuildConfig.GEOAPIFY_API_KEY)
                        .build()
                    chain.proceed(original.newBuilder().url(url).build())
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

// ---------------- Wikipedia (ảnh cho thành phố và địa danh) ----------------
data class WikiResponse(@SerializedName("query") val query: WikiQuery? = null)
data class WikiQuery(@SerializedName("pages") val pages: Map<String, WikiPage>? = null)
data class WikiPage(
    @SerializedName("title") val title: String? = null,
    /** Thứ tự gần nhất khi tìm theo toạ độ (1 = gần nhất) */
    @SerializedName("index") val index: Int? = null,
    @SerializedName("thumbnail") val thumbnail: WikiThumbnail? = null
)
data class WikiThumbnail(@SerializedName("source") val source: String? = null)

interface WikipediaApiService {

    @GET("w/api.php")
    suspend fun fetchImage(
        @Query("titles") title: String,
        @Query("action") action: String = "query",
        @Query("prop") prop: String = "pageimages",
        @Query("format") format: String = "json",
        @Query("pithumbsize") thumbSize: Int = 800,
        @Query("redirects") redirects: Int = 1
    ): WikiResponse

    /** Tìm bài viết Wikipedia gần một toạ độ và lấy ảnh đại diện */
    @GET("w/api.php")
    suspend fun imageNear(
        @Query("ggscoord") coord: String,          // "lat|lon"
        @Query("ggsradius") radius: Int = 200,     // mét
        @Query("ggslimit") limit: Int = 5,
        @Query("action") action: String = "query",
        @Query("generator") generator: String = "geosearch",
        @Query("prop") prop: String = "pageimages",
        @Query("format") format: String = "json",
        @Query("pithumbsize") thumbSize: Int = 600
    ): WikiResponse

    companion object {
        fun create(lang: String): WikipediaApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", "TravelApp/1.0 (Android)")
                            .build()
                    )
                }
                .build()

            return Retrofit.Builder()
                .baseUrl("https://$lang.wikipedia.org/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WikipediaApiService::class.java)
        }
    }
}
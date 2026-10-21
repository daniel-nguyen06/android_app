package android_app

import com.travel.android_app.BuildConfig
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.util.Locale

class PlaceRepository(
    private val apiService: PlacesApiService = PlacesApiService.create(),
    private val aiParser: AiQueryParser = AiQueryParser(geminiApiKey = BuildConfig.GEMINI_API_KEY)
) {

    enum class SortType { DISTANCE, RATING_DESC, PRICE_ASC }

    private companion object {
        const val NEARBY_RADIUS_METERS = 5000.0
    }

    suspend fun fetchPlaces(
        keyword: String,
        categoryType: String? = null, // id loại của Google: restaurant, cafe, lodging...
        userLat: Double? = null,
        userLng: Double? = null
    ): List<PlaceItem> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return emptyList()

        try {
            // 1. Gemini chuyển câu nhập thành câu tìm kiếm cho Google Maps
            val parsed = aiParser.parseUserPrompt(trimmed)
            val searchQuery = parsed.searchQuery?.takeIf { it.isNotBlank() } ?: trimmed

            // 2. Chỉ ưu tiên khu vực quanh người dùng khi họ muốn tìm gần và đã có vị trí
            val bias = if (parsed.nearby && userLat != null && userLng != null) {
                LocationBias(Circle(LatLngLiteral(userLat, userLng), NEARBY_RADIUS_METERS))
            } else null

            println(">>> [Places] query='$searchQuery', nearby=${parsed.nearby}, type=$categoryType")

            // 3. Gọi Places API (New)
            val response = apiService.searchText(
                TextSearchRequest(
                    textQuery = searchQuery,
                    languageCode = Locale.getDefault().language,
                    includedType = categoryType,
                    locationBias = bias
                )
            )
            val places = response.places.orEmpty()
            println(">>> [Google Places New] count=${places.size}")

            return places.mapNotNull { it.toPlaceItem(userLat, userLng) }

        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string().orEmpty().take(300)
            println(">>> [Google Places New] HTTP ${e.code()} $body")
            throw IllegalStateException("Google Places: HTTP ${e.code()} $body".trim())
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    fun calculateDistances(list: List<PlaceItem>, userLat: Double?, userLng: Double?) {
        if (userLat == null || userLng == null) return
        for (item in list) {
            val loc = item.geometry?.location
            if (loc != null) {
                item.distanceInKm = haversineKm(userLat, userLng, loc.lat, loc.lng)
            }
        }
    }

    fun filterPlaces(list: List<PlaceItem>, minRating: Double = 0.0, maxPriceLevel: Int = 4): List<PlaceItem> {
        return list.filter { it.rating >= minRating && it.priceLevel <= maxPriceLevel }
    }

    fun sortPlaces(list: List<PlaceItem>, sortBy: SortType): List<PlaceItem> {
        return when (sortBy) {
            SortType.DISTANCE -> list.sortedBy { it.distanceInKm }
            SortType.RATING_DESC -> list.sortedByDescending { it.rating }
            SortType.PRICE_ASC -> list.sortedBy { it.priceLevel }
        }
    }
}
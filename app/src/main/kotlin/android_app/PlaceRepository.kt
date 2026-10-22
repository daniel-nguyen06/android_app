package android_app

import com.travel.android_app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.HttpException
import java.util.Locale

class PlaceRepository(
    private val apiService: PlacesApiService = PlacesApiService.create(),
    private val aiParser: AiQueryParser = AiQueryParser(geminiApiKey = BuildConfig.GEMINI_API_KEY)
) {

    enum class SortType { DISTANCE, RATING_DESC, PRICE_ASC }

    private companion object {
        const val NEARBY_RADIUS_METERS = 5000
        const val AREA_FALLBACK_RADIUS_METERS = 10000
        const val DEFAULT_CATEGORIES = "tourism.sights,beach,leisure.park,entertainment.culture"
    }

    private val wikiCache = mutableMapOf<String, WikipediaApiService>()
    private val imageCache = mutableMapOf<String, String>()

    suspend fun fetchPlaces(
        keyword: String,
        categoryType: String? = null,
        userLat: Double? = null,
        userLng: Double? = null
    ): List<PlaceItem> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return emptyList()

        try {
            val parsed = aiParser.parseUserPrompt(trimmed)
            val lang = Locale.getDefault().language
            val categories = categoryType ?: parsed.categories ?: DEFAULT_CATEGORIES

            var filter: String? = null
            var bias: String? = null
            var cityItem: PlaceItem? = null

            if (parsed.nearby && userLat != null && userLng != null) {
                filter = "circle:$userLng,$userLat,$NEARBY_RADIUS_METERS"
                bias = "proximity:$userLng,$userLat"
            } else if (!parsed.location.isNullOrBlank()) {
                val geo = apiService.geocode(parsed.location, lang).features.orEmpty().firstOrNull()
                val gp = geo?.properties
                if (geo == null || gp?.lat == null || gp.lon == null) {
                    throw IllegalStateException("Không tìm thấy khu vực \"${parsed.location}\"")
                }
                val b = geo.bbox
                filter = if (b != null && b.size == 4) {
                    "rect:${b[0]},${b[1]},${b[2]},${b[3]}"
                } else {
                    "circle:${gp.lon},${gp.lat},$AREA_FALLBACK_RADIUS_METERS"
                }
                bias = "proximity:${gp.lon},${gp.lat}"

                // Chỉ nhập tên địa danh -> ghim thẻ thành phố ở đầu danh sách
                if (parsed.destinationOnly && parsed.name.isNullOrBlank() && geo.isCityLike()) {
                    val image = fetchWikiImage(geo.areaName() ?: parsed.location, lang)
                    cityItem = geo.toCityItem(image, userLat, userLng)
                }
            } else if (userLat != null && userLng != null) {
                filter = "circle:$userLng,$userLat,$NEARBY_RADIUS_METERS"
                bias = "proximity:$userLng,$userLat"
            } else {
                throw IllegalStateException("Hãy nhập khu vực muốn tìm (ví dụ: cà phê Nha Trang) hoặc bật vị trí.")
            }

            println(">>> [Places] categories=$categories, name=${parsed.name}, filter=$filter, city=${cityItem?.name}")

            val features = apiService.searchPlaces(
                categories = categories,
                filter = filter,
                bias = bias,
                name = parsed.name?.takeIf { it.isNotBlank() },
                lang = lang
            ).features.orEmpty()
            println(">>> [Geoapify] count=${features.size}")

            val places = features.mapNotNull { it.toPlaceItem(userLat, userLng) }
            return listOfNotNull(cityItem) + places

        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string().orEmpty().take(300)
            println(">>> [Geoapify] HTTP ${e.code()} $body")
            throw IllegalStateException("Geoapify: HTTP ${e.code()} $body".trim())
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /** Lấy ảnh đại diện từ Wikipedia theo tên; lỗi hoặc không có ảnh thì trả chuỗi rỗng */
    private suspend fun fetchWikiImage(title: String, lang: String): String {
        return try {
            val api = wikiCache.getOrPut(lang) { WikipediaApiService.create(lang) }
            api.fetchImage(title).query?.pages?.values
                ?.firstNotNullOfOrNull { it.thumbnail?.source }
                .orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Bổ sung ảnh từ Wikipedia cho các địa điểm chưa có ảnh (tối đa 12 nơi đầu danh sách).
     * Lỗi mạng hoặc không có ảnh thì giữ nguyên, không ném lỗi.
     */
    suspend fun enrichImages(places: List<PlaceItem>, max: Int = 12): List<PlaceItem> {
        val targets = places
            .filter { it.imageUrl.isBlank() && it.geometry?.location != null }
            .take(max)
            .map { it.placeId }
            .toSet()
        if (targets.isEmpty()) return places

        val lang = Locale.getDefault().language
        val gate = Semaphore(4) // tối đa 4 yêu cầu song song

        return coroutineScope {
            places.map { place ->
                async {
                    if (place.placeId !in targets) return@async place
                    imageCache[place.placeId]?.let { cached ->
                        return@async if (cached.isNotBlank()) place.copy(imageUrl = cached) else place
                    }
                    val loc = place.geometry?.location ?: return@async place
                    val url = gate.withPermit { wikiImageNear(loc.lat, loc.lng, lang) }
                    imageCache[place.placeId] = url
                    if (url.isNotBlank()) place.copy(imageUrl = url) else place
                }
            }.awaitAll()
        }
    }

    private suspend fun wikiImageNear(lat: Double, lng: Double, lang: String): String {
        suspend fun query(l: String): String = try {
            val api = wikiCache.getOrPut(l) { WikipediaApiService.create(l) }
            api.imageNear("$lat|$lng").query?.pages?.values
                ?.filter { it.thumbnail?.source != null }
                ?.minByOrNull { it.index ?: Int.MAX_VALUE }
                ?.thumbnail?.source.orEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ""
        }

        val primary = query(lang)
        return if (primary.isNotBlank() || lang == "en") primary else query("en")
    }

    fun calculateDistances(list: List<PlaceItem>, userLat: Double?, userLng: Double?) {
        if (userLat == null || userLng == null) return
        for (item in list) {
            val loc = item.geometry?.location
            if (loc != null) item.distanceInKm = haversineKm(userLat, userLng, loc.lat, loc.lng)
        }
    }

    // Thẻ thành phố luôn được giữ lại và ghim ở đầu, không bị bộ lọc/sắp xếp đẩy đi
    private fun isPinned(item: PlaceItem) = CITY_PLACE_TYPE in item.types

    fun filterPlaces(list: List<PlaceItem>, minRating: Double = 0.0, maxPriceLevel: Int = 4): List<PlaceItem> {
        return list.filter { isPinned(it) || (it.rating >= minRating && it.priceLevel <= maxPriceLevel) }
    }

    fun sortPlaces(list: List<PlaceItem>, sortBy: SortType): List<PlaceItem> {
        val (pinned, rest) = list.partition { isPinned(it) }
        val sorted = when (sortBy) {
            SortType.DISTANCE -> rest.sortedBy { it.distanceInKm }
            SortType.RATING_DESC -> rest.sortedByDescending { it.rating }
            SortType.PRICE_ASC -> rest.sortedBy { it.priceLevel }
        }
        return pinned + sorted
    }
}
package android_app

import kotlin.math.*

class PlaceRepository(
    private val apiService: PlacesApiService = PlacesApiService.create(),
    private val wikiApi: WikipediaApiService = WikipediaApiService.create(),
    private val aiParser: AiQueryParser = AiQueryParser(geminiApiKey = "AQ.Ab8RN6KN_pdWeE38ANxu9Os1B1JmyzoNWOwU104ckgUj9KEyOw"),
    private val apiKey: String = "2578b45979c64af6a1c192e486b9181d"
){

    enum class SortType { DISTANCE, RATING_DESC, PRICE_ASC }

    suspend fun fetchPlaces(
        keyword: String,
        categoryType: String? = null,
        userLat: Double? = null,
        userLng: Double? = null
    ): List<PlaceItem> {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            // 1. Cho Gemini AI phân tích câu gõ tự nhiên của người dùng
            val aiResult = aiParser.parseUserPrompt(trimmed)
            println(">>> [AI Phân tích] Brand: '${aiResult.cleanName}', Category: '${aiResult.category}', LocationHint: '${aiResult.locationHint}'")

            val biasParam = if (userLat != null && userLng != null) {
                "proximity:$userLng,$userLat"
            } else null

            // 2. Ném các trường đã bóc tách vào Geoapify
            var response = apiService.searchPlaces(
                categories = aiResult.category,
                name = aiResult.cleanName,
                bias = biasParam,
                apiKey = apiKey
            )

            // 3. Fallback geocode nếu tìm theo name chính xác chưa ra
            if (response.features.isEmpty()) {
                val searchText = aiResult.cleanName ?: trimmed
                response = apiService.geocodeSearch(
                    text = searchText,
                    bias = biasParam,
                    apiKey = apiKey
                )
            }

            // 4. Map dữ liệu sang PlaceItem
            response.features.mapNotNull { feature ->
                val prop = feature.properties ?: return@mapNotNull null
                val name = prop.name ?: prop.addressLine1 ?: return@mapNotNull null

                val distKm = when {
                    prop.distanceMeters != null -> Math.round(prop.distanceMeters / 10.0) / 100.0
                    userLat != null && userLng != null -> calculateHaversine(userLat, userLng, prop.lat, prop.lon)
                    else -> 0.0
                }

                val hours = prop.openingHours ?: "07:30 - 22:30 (Hàng ngày)"
                val isCafe = aiResult.category.contains("cafe")
                val budget = if (isCafe) "30.000đ - 65.000đ" else "60.000đ - 250.000đ"
                val calcRating = 4.2 + (abs(prop.placeId.hashCode()) % 8) / 10.0

                val realImageUrl = getWikiImageOrDefault(name, aiResult.category, prop.placeId.hashCode())

                PlaceItem(
                    placeId = prop.placeId ?: java.util.UUID.randomUUID().toString(),
                    name = name,
                    vicinity = prop.formatted ?: "${prop.addressLine1 ?: ""}, ${prop.city ?: "Việt Nam"}",
                    rating = calcRating,
                    priceLevel = 2,
                    estimatedBudget = budget,
                    openingHours = hours,
                    description = "Quán có đánh giá cao. Liên hệ: ${prop.contact?.phone ?: prop.website ?: "Đang cập nhật"}",
                    imageUrl = realImageUrl,
                    types = prop.categories,
                    geometry = Geometry(location = LatLng(prop.lat, prop.lon)),
                    distanceInKm = distKm
                )
            }.sortedBy { it.distanceInKm }

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun getWikiImageOrDefault(placeName: String, category: String, seed: Int): String {
        return try {
            val response = wikiApi.fetchPlaceImage(title = placeName)
            val pages = response.query?.pages
            val thumbUrl = pages?.values?.firstOrNull()?.thumbnail?.source
            if (!thumbUrl.isNullOrBlank()) thumbUrl else getFallbackImage(category, seed)
        } catch (e: Exception) {
            getFallbackImage(category, seed)
        }
    }

    private fun getFallbackImage(category: String, seed: Int): String {
        val fallbackImages = if (category.contains("cafe")) {
            listOf(
                "[https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=800](https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=800)",
                "[https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=800](https://images.unsplash.com/photo-1501339847302-ac426a4a7cbb?w=800)"
            )
        } else {
            listOf(
                "[https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800](https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?w=800)",
                "[https://images.unsplash.com/photo-1552566626-52f8b828add9?w=800](https://images.unsplash.com/photo-1552566626-52f8b828add9?w=800)"
            )
        }
        return fallbackImages[abs(seed) % fallbackImages.size]
    }

    fun calculateDistances(list: List<PlaceItem>, userLat: Double?, userLng: Double?) {
        if (userLat == null || userLng == null) return
        for (item in list) {
            val loc = item.geometry?.location
            if (loc != null) {
                item.distanceInKm = calculateHaversine(userLat, userLng, loc.lat, loc.lng)
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

    private fun calculateHaversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return Math.round(r * c * 100.0) / 100.0
    }
}
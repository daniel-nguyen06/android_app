package android_app

import kotlin.math.*

class TravelBackendService(
    private val placesApi: PlacesApiService = PlacesApiService.create(),
    private val weatherApi: WeatherApiService = WeatherApiService.create(),
    private val geoapifyKey: String = "dán_api_key_geoapify_của_bạn_vào_đây", // <-- DÁN KEY VÀO ĐÂY
    private val weatherKey: String = ""
) {

    // === CHỨC NĂNG TÌM KIẾM, LỌC VÀ LẤY ĐỊA ĐIỂM TỪ GEOAPIFY ===
    suspend fun searchAndFilterPlaces(
        query: String,
        category: String? = null,
        minRating: Double = 0.0,
        userLat: Double? = null,
        userLng: Double? = null
    ): List<PlaceItem> {
        val geoCategory = when {
            query.contains("cà phê", ignoreCase = true) || query.contains("cafe", ignoreCase = true) -> "catering.cafe"
            query.contains("ăn", ignoreCase = true) || query.contains("nhà hàng", ignoreCase = true) -> "catering.restaurant"
            query.contains("khách sạn", ignoreCase = true) -> "accommodation.hotel"
            else -> "catering,tourism,entertainment"
        }

        val biasParam = if (userLat != null && userLng != null) {
            "proximity:$userLng,$userLat"
        } else null

        val response = placesApi.searchPlaces(
            categories = geoCategory,
            name = if (query.isNotBlank() && !query.equals("cà phê", true)) query else null,
            bias = biasParam,
            apiKey = geoapifyKey
        )

        val places = response.features.mapNotNull { feature ->
            val prop = feature.properties ?: return@mapNotNull null
            val name = prop.name ?: prop.addressLine1 ?: return@mapNotNull null

            val distKm = when {
                prop.distanceMeters != null -> Math.round(prop.distanceMeters / 10.0) / 100.0
                userLat != null && userLng != null -> calculateHaversine(userLat, userLng, prop.lat, prop.lon)
                else -> 0.0
            }

            val hours = prop.openingHours ?: "07:30 - 22:30 (Hàng ngày)"
            val dynamicImage = "https://picsum.photos/seed/${prop.placeId.hashCode()}/700/450"
            val budget = if (geoCategory.contains("cafe")) "30.000đ - 65.000đ" else "60.000đ - 250.000đ"
            val calcRating = 4.0 + (abs(prop.placeId.hashCode()) % 10) / 10.0

            PlaceItem(
                placeId = prop.placeId ?: java.util.UUID.randomUUID().toString(),
                name = name,
                vicinity = prop.formatted ?: "${prop.addressLine1 ?: ""}, ${prop.city ?: "Việt Nam"}",
                rating = calcRating,
                priceLevel = 2,
                estimatedBudget = budget,
                openingHours = hours,
                description = "Quán có đánh giá tốt tại khu vực. Liên hệ: ${prop.contact?.phone ?: prop.website ?: "Đang cập nhật"}",
                imageUrl = dynamicImage,
                types = prop.categories,
                geometry = Geometry(location = LatLng(prop.lat, prop.lon)),
                distanceInKm = distKm
            )
        }

        return places.filter { it.rating >= minRating }
    }

    // === CHỨC NĂNG THỜI TIẾT ===
    suspend fun getPlaceWeather(lat: Double, lng: Double): WeatherResponse? {
        return try {
            if (weatherKey.isNotBlank()) {
                weatherApi.getCurrentWeather(lat = lat, lon = lng, apiKey = weatherKey)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
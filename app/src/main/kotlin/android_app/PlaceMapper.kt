package android_app

import java.util.Calendar
import kotlin.math.*

internal fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return Math.round(r * c * 100.0) / 100.0
}

private fun priceLevelToInt(level: String?): Int = when (level) {
    "PRICE_LEVEL_FREE" -> 0
    "PRICE_LEVEL_INEXPENSIVE" -> 1
    "PRICE_LEVEL_MODERATE" -> 2
    "PRICE_LEVEL_EXPENSIVE" -> 3
    "PRICE_LEVEL_VERY_EXPENSIVE" -> 4
    else -> 0
}

private fun openingText(hours: NewOpeningHours?): String {
    if (hours == null) return ""
    val status = when (hours.openNow) {
        true -> "Đang mở cửa"
        false -> "Đã đóng cửa"
        null -> ""
    }
    // weekdayDescriptions bắt đầu từ Thứ Hai (index 0)
    val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) // CN=1, T2=2...
    val today = hours.weekdayDescriptions?.getOrNull((dow + 5) % 7).orEmpty()
    return listOf(status, today).filter { it.isNotBlank() }.joinToString(" · ")
}

/** Chuyển dữ liệu Places API (New) sang model hiển thị. Trả null nếu thiếu dữ liệu bắt buộc. */
internal fun NewPlace.toPlaceItem(userLat: Double?, userLng: Double?): PlaceItem? {
    val placeId = id ?: return null
    val name = displayName?.text ?: return null
    val lat = location?.latitude ?: return null
    val lng = location.longitude

    val distKm = if (userLat != null && userLng != null) haversineKm(userLat, userLng, lat, lng) else 0.0
    val level = priceLevelToInt(priceLevel)
    val imageUrl = photos?.firstOrNull { !it.name.isNullOrBlank() }
        ?.name?.let { PlacesApiService.photoUrl(it) }.orEmpty()
    val address = formattedAddress.orEmpty()

    return PlaceItem(
        placeId = placeId,
        name = name,
        vicinity = address,
        rating = rating ?: 0.0,
        priceLevel = level,
        estimatedBudget = if (level > 0) "$".repeat(level) else "",
        openingHours = openingText(currentOpeningHours ?: regularOpeningHours),
        description = address,
        imageUrl = imageUrl,
        types = types ?: emptyList(),
        geometry = Geometry(location = LatLng(lat, lng)),
        distanceInKm = distKm
    )
}
package android_app

import kotlin.math.*

/** Đánh dấu một PlaceItem là thành phố/khu vực (hiển thị ghim ở đầu danh sách) */
const val CITY_PLACE_TYPE = "place.city"

private val CITY_RESULT_TYPES = setOf("city", "county", "state", "district", "suburb", "locality", "country")

internal fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return Math.round(r * c * 100.0) / 100.0
}

/** Kết quả geocode có phải là một thành phố/khu vực không */
internal fun GeoapifyFeature.isCityLike(): Boolean =
    properties?.resultType in CITY_RESULT_TYPES

/** Tên hiển thị của khu vực từ kết quả geocode */
internal fun GeoapifyFeature.areaName(): String? =
    properties?.let { it.name ?: it.city ?: it.addressLine1 }?.takeIf { it.isNotBlank() }

/** Tạo thẻ thành phố từ kết quả geocode */
internal fun GeoapifyFeature.toCityItem(imageUrl: String, userLat: Double?, userLng: Double?): PlaceItem? {
    val p = properties ?: return null
    val name = areaName() ?: return null
    val lat = p.lat ?: return null
    val lng = p.lon ?: return null
    val address = p.formatted.orEmpty()

    return PlaceItem(
        placeId = p.placeId ?: "city:$lat,$lng",
        name = name,
        vicinity = address,
        description = address,
        imageUrl = imageUrl,
        types = listOf(CITY_PLACE_TYPE),
        geometry = Geometry(location = LatLng(lat, lng)),
        distanceInKm = if (userLat != null && userLng != null) haversineKm(userLat, userLng, lat, lng) else 0.0
    )
}

/** Chuyển dữ liệu Geoapify/OSM sang model hiển thị. Bỏ qua địa điểm không có tên hoặc toạ độ. */
internal fun GeoapifyFeature.toPlaceItem(userLat: Double?, userLng: Double?): PlaceItem? {
    val p = properties ?: return null
    val name = p.name?.takeIf { it.isNotBlank() } ?: return null
    val lat = p.lat ?: return null
    val lng = p.lon ?: return null
    val raw = p.datasource?.raw

    val distKm = if (userLat != null && userLng != null) haversineKm(userLat, userLng, lat, lng) else 0.0
    val address = p.formatted ?: p.addressLine2.orEmpty()
    val hours = p.openingHours ?: (raw?.get("opening_hours") as? String).orEmpty()
    val image = (raw?.get("image") as? String)?.takeIf { it.startsWith("http") }.orEmpty()

    return PlaceItem(
        placeId = p.placeId ?: "$lat,$lng",
        name = name,
        vicinity = address,
        rating = 0.0,
        priceLevel = 0,
        estimatedBudget = "",
        openingHours = hours,
        description = address,
        imageUrl = image,
        types = p.categories.orEmpty(),
        geometry = Geometry(location = LatLng(lat, lng)),
        distanceInKm = distKm
    )
}
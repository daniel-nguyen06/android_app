package android_app

<<<<<<< HEAD
import com.google.gson.annotations.SerializedName

// Cấu trúc trả về tổng quát từ Google Places TextSearch / NearbySearch
data class PlacesSearchResponse(
    @SerializedName("results") val results: List<PlaceItem>,
    @SerializedName("status") val status: String
)

data class PlaceItem(
    @SerializedName("place_id") val placeId: String,
    @SerializedName("name") val name: String,
    @SerializedName("rating") val rating: Double = 0.0,
    @SerializedName("price_level") val priceLevel: Int = 0, // 0-4 (Đại diện cho cost)
    @SerializedName("vicinity") val vicinity: String? = null,
    @SerializedName("types") val types: List<String> = emptyList(),
    @SerializedName("geometry") val geometry: Geometry? = null
) {
    // Thuộc tính tính toán khoảng cách (Frontend sẽ dùng để hiển thị)
    var distanceInKm: Double = 0.0
}

data class Geometry(
    @SerializedName("location") val location: LocationLatLng
)

data class LocationLatLng(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
=======
/**
 * Model ánh xạ cho UI hiển thị danh sách địa điểm khám phá.
 */
data class PlaceUiModel(
    val id: String,
    val name: String,
    val rating: Double,
    val vicinity: String?,
    val distanceKm: Double,
    val priceLevel: Int
>>>>>>> origin/fix/database
)
package android_app

sealed interface PlaceScreenStatus {
    data object Idle : PlaceScreenStatus
    data object Loading : PlaceScreenStatus
    data class Success(val places: List<PlaceItem>) : PlaceScreenStatus
    data object Empty : PlaceScreenStatus
    data class Error(val message: String) : PlaceScreenStatus
}

/** Một nhóm địa điểm gần người dùng (ví dụ: Tham quan, Cà phê) */
data class NearbySection(
    val categoryId: String,
    val title: String,
    val places: List<PlaceItem>
)

data class PlaceUiState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val minRating: Double = 0.0,
    val maxPriceLevel: Int = 4,
    val sortType: PlaceRepository.SortType = PlaceRepository.SortType.DISTANCE,

    /** null cho đến khi lấy được vị trí thật từ LocationTracker */
    val userLat: Double? = null,
    val userLng: Double? = null,

    val favoritePlaceIds: Set<String> = emptySet(),
    /** Lưu cả địa điểm để tab Yêu thích không mất dữ liệu khi tìm từ khóa khác */
    val favoritePlaces: Map<String, PlaceItem> = emptyMap(),

    val selectedPlace: PlaceItem? = null,
    val status: PlaceScreenStatus = PlaceScreenStatus.Idle,

    // --- Gợi ý quanh người dùng ---
    val areaName: String? = null,
    val nearbySections: List<NearbySection> = emptyList(),
    val nearbyLoading: Boolean = false,
    val nearbyError: String? = null
)
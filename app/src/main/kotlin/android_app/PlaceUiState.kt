package android_app

sealed interface PlaceScreenStatus {
    data object Idle : PlaceScreenStatus
    data object Loading : PlaceScreenStatus
    data class Success(val places: List<PlaceItem>) : PlaceScreenStatus
    data object Empty : PlaceScreenStatus
    data class Error(val message: String) : PlaceScreenStatus
}

data class PlaceUiState(
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val minRating: Double = 0.0,
    val maxPriceLevel: Int = 4,
    val sortType: PlaceRepository.SortType = PlaceRepository.SortType.RATING_DESC,

    /** null cho đến khi lấy được vị trí thật từ LocationTracker/LocationHelper */
    val userLat: Double? = null,
    val userLng: Double? = null,

    val favoritePlaceIds: Set<String> = emptySet(),
    val selectedPlace: PlaceItem? = null,
    val status: PlaceScreenStatus = PlaceScreenStatus.Idle
)
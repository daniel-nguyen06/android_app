package android_app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaceViewModel(
    private val repository: PlaceRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {
    private val _uiState = MutableStateFlow(PlaceUiState())
    val uiState: StateFlow<PlaceUiState> = _uiState.asStateFlow()

    private var loadDataJob: Job? = null
    private var searchJob: Job? = null
    private var rawPlacesList: List<PlaceItem> = emptyList()

    fun updateUserLocation(lat: Double, lng: Double) {
        _uiState.update { it.copy(userLat = lat, userLng = lng) }
    }

    fun onEvent(event: PlaceUiEvent) {
        when (event) {
            is PlaceUiEvent.OnSearchQueryChanged -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                searchJob?.cancel()
                searchJob = scope.launch {
                    delay(600) // debounce
                    loadPlaces()
                }
            }
            is PlaceUiEvent.OnCategorySelected -> {
                _uiState.update { it.copy(selectedCategory = event.category) }
                if (_uiState.value.searchQuery.isNotBlank()) loadPlaces()
            }
            is PlaceUiEvent.OnFilterRatingChanged -> {
                _uiState.update { it.copy(minRating = event.minRating) }
                applyLocalFiltersAndSort()
            }
            is PlaceUiEvent.OnFilterPriceChanged -> {
                _uiState.update { it.copy(maxPriceLevel = event.maxPriceLevel) }
                applyLocalFiltersAndSort()
            }
            is PlaceUiEvent.OnSortTypeChanged -> {
                _uiState.update { it.copy(sortType = event.sortType) }
                applyLocalFiltersAndSort()
            }
            is PlaceUiEvent.OnToggleFavorite -> toggleFavorite(event.placeId)
            is PlaceUiEvent.OnPlaceSelected -> {
                _uiState.update { it.copy(selectedPlace = event.place) }
            }
            PlaceUiEvent.OnClearFilters -> {
                _uiState.update {
                    it.copy(
                        minRating = 0.0,
                        maxPriceLevel = 4,
                        sortType = PlaceRepository.SortType.DISTANCE
                    )
                }
                applyLocalFiltersAndSort()
            }
            PlaceUiEvent.OnRetry -> loadPlaces()
        }
    }

    fun loadPlaces() {
        loadDataJob?.cancel()
        loadDataJob = scope.launch {
            val currentState = _uiState.value
            val keyword = currentState.searchQuery.trim()

            if (keyword.isBlank()) {
                rawPlacesList = emptyList()
                _uiState.update { it.copy(status = PlaceScreenStatus.Idle) }
                return@launch
            }

            _uiState.update { it.copy(status = PlaceScreenStatus.Loading) }

            try {
                val results = withContext(Dispatchers.IO) {
                    repository.fetchPlaces(
                        keyword = keyword,
                        categoryType = currentState.selectedCategory,
                        userLat = currentState.userLat,
                        userLng = currentState.userLng
                    )
                }

                repository.calculateDistances(results, currentState.userLat, currentState.userLng)
                rawPlacesList = results
                applyLocalFiltersAndSort()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        status = PlaceScreenStatus.Error(
                            e.localizedMessage ?: "Không thể kết nối đến máy chủ. Vui lòng thử lại!"
                        )
                    )
                }
            }
        }
    }

    private fun applyLocalFiltersAndSort() {
        val s = _uiState.value
        val filtered = repository.filterPlaces(rawPlacesList, s.minRating, s.maxPriceLevel)
        val sorted = repository.sortPlaces(filtered, s.sortType)
        _uiState.update {
            it.copy(status = if (sorted.isEmpty()) PlaceScreenStatus.Empty else PlaceScreenStatus.Success(sorted))
        }
    }

    private fun toggleFavorite(placeId: String) {
        _uiState.update { state ->
            val ids = state.favoritePlaceIds.toMutableSet()
            val places = state.favoritePlaces.toMutableMap()
            if (ids.remove(placeId)) {
                places.remove(placeId)
            } else {
                ids.add(placeId)
                val place = rawPlacesList.find { it.placeId == placeId }
                    ?: state.selectedPlace?.takeIf { it.placeId == placeId }
                if (place != null) places[placeId] = place
            }
            state.copy(favoritePlaceIds = ids, favoritePlaces = places)
        }
    }
}
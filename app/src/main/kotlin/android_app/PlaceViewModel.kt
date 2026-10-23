package android_app

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

class PlaceViewModel(
    private val repository: PlaceRepository,
    private val favoriteRepository: FavoriteRepository = FavoriteRepository(),
    private val scope: CoroutineScope =
        CoroutineScope(Dispatchers.Main + Job())
) {
    private val auth = FirebaseAuth.getInstance()

    private val authStateListener =
        FirebaseAuth.AuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid

            if (uid == null) {
                _uiState.update {
                    it.copy(
                        favoritePlaceIds = emptySet(),
                        favoritePlaces = emptyMap()
                    )
                }
            } else {
                loadFavorites(uid)
            }
        }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    private fun loadFavorites(userId: String) {
        scope.launch {
            try {
                val favorites = favoriteRepository.getFavorites(userId)

                // Không ghi đè trạng thái nếu tài khoản đã đổi trong lúc tải.
                if (auth.currentUser?.uid == userId) {
                    _uiState.update {
                        it.copy(
                            favoritePlaceIds = favorites.keys,
                            favoritePlaces = favorites
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("PlaceViewModel", "Không tải được địa điểm yêu thích", e)
            }
        }
    }

    fun dispose() {
        auth.removeAuthStateListener(authStateListener)
        scope.cancel()
    }

    private companion object {
        const val NEARBY_SECTION_COUNT = 4
        const val RELOAD_DISTANCE_KM = 1.0
    }

    private val _uiState = MutableStateFlow(PlaceUiState())
    val uiState: StateFlow<PlaceUiState> = _uiState.asStateFlow()

    private var loadDataJob: Job? = null
    private var searchJob: Job? = null
    private var nearbyJob: Job? = null
    private var nearbyLoadedFor: Pair<Double, Double>? = null
    private var rawPlacesList: List<PlaceItem> = emptyList()

    /** Gọi khi có vị trí mới; tự tải gợi ý quanh người dùng nếu chưa có hoặc đã di chuyển xa */
    fun updateUserLocation(lat: Double, lng: Double) {
        _uiState.update { it.copy(userLat = lat, userLng = lng) }
        val last = nearbyLoadedFor
        if (last == null || haversineKm(last.first, last.second, lat, lng) > RELOAD_DISTANCE_KM) {
            loadNearby()
        }
    }

    /** Tải các nhóm địa điểm quanh vị trí người dùng */
    fun loadNearby() {
        val s = _uiState.value
        val lat = s.userLat ?: return
        val lng = s.userLng ?: return

        nearbyJob?.cancel()
        nearbyJob = scope.launch {
            _uiState.update { it.copy(nearbyLoading = true, nearbyError = null) }
            try {
                val categories = placeCategories
                    .mapNotNull { c -> c.id?.let { id -> c.label to id } }
                    .take(NEARBY_SECTION_COUNT)

                var firstError: String? = null

                val (area, sections) = withContext(Dispatchers.IO) {
                    coroutineScope {
                        val areaJob = async { repository.reverseAreaName(lat, lng) }
                        val sectionJobs = categories.map { (label, id) ->
                            async {
                                try {
                                    NearbySection(id, label, repository.fetchNearby(id, lat, lng))
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    println(">>> [Nearby] $label lỗi: ${e.message}")
                                    if (firstError == null) firstError = e.localizedMessage
                                    NearbySection(id, label, emptyList())
                                }
                            }
                        }
                        areaJob.await() to sectionJobs.awaitAll()
                    }
                }

                val nonEmpty = sections.filter { it.places.isNotEmpty() }
                nearbyLoadedFor = lat to lng
                _uiState.update {
                    it.copy(
                        areaName = area,
                        nearbySections = nonEmpty,
                        nearbyLoading = false,
                        nearbyError = if (nonEmpty.isEmpty()) firstError else null
                    )
                }

                // Tải ảnh ở nền rồi cập nhật lại các nhóm
                if (nonEmpty.isNotEmpty()) {
                    val flat = nonEmpty.flatMap { it.places }.distinctBy { it.placeId }
                    val enriched = withContext(Dispatchers.IO) {
                        repository.enrichImages(flat, max = 24)
                    }.associateBy { it.placeId }

                    _uiState.update { st ->
                        st.copy(
                            nearbySections = st.nearbySections.map { sec ->
                                sec.copy(places = sec.places.map { p -> enriched[p.placeId] ?: p })
                            }
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        nearbyLoading = false,
                        nearbyError = e.localizedMessage ?: "Không thể tải địa điểm quanh bạn."
                    )
                }
            }
        }
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
                applyLocalFiltersAndSort() // hiện danh sách ngay

                // Tải ảnh ở nền rồi cập nhật lại danh sách
                val enriched = withContext(Dispatchers.IO) { repository.enrichImages(results) }
                rawPlacesList = enriched
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
        val userId = auth.currentUser?.uid ?: return
        val currentState = _uiState.value
        val wasFavorite = placeId in currentState.favoritePlaceIds

        val place = currentState.favoritePlaces[placeId]
            ?: rawPlacesList.find { it.placeId == placeId }
            ?: currentState.nearbySections.flatMap { it.places }.find { it.placeId == placeId }
            ?: currentState.selectedPlace?.takeIf {
                it.placeId == placeId
            }

        if (!wasFavorite && place == null) {
            Log.w(
                "PlaceViewModel",
                "Không tìm thấy dữ liệu địa điểm để lưu: $placeId"
            )
            return
        }

        scope.launch {
            try {
                if (wasFavorite) {
                    favoriteRepository.removeFavorite(userId, placeId)

                    _uiState.update { state ->
                        state.copy(
                            favoritePlaceIds =
                                state.favoritePlaceIds - placeId,
                            favoritePlaces =
                                state.favoritePlaces - placeId
                        )
                    }
                } else {
                    val placeToSave = requireNotNull(place)

                    favoriteRepository.addFavorite(userId, placeToSave)

                    _uiState.update { state ->
                        state.copy(
                            favoritePlaceIds =
                                state.favoritePlaceIds + placeId,
                            favoritePlaces =
                                state.favoritePlaces +
                                        (placeId to placeToSave)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "PlaceViewModel",
                    "Không thể cập nhật địa điểm yêu thích",
                    e
                )
            }
        }
    }
}
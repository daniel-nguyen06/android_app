package android_app

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import android_app.ui.theme.SunsetCoral
import android_app.ui.theme.TravelAppTheme

/**
 * Màn hình Khám phá & Tìm kiếm Địa điểm Du lịch.
 * Tách biệt hoàn toàn Stateless Composable UI và State/ViewModel theo Clean Architecture (UDF).
 */

@Composable
fun PlaceDiscoveryRoute(
    viewModel: PlaceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    PlaceDiscoveryScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDiscoveryScreen(
    uiState: PlaceUiState,
    onEvent: (PlaceUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Explore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Khám Phá Địa Điểm",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* TODO: Check-in action */ },
                containerColor = SunsetCoral,
                contentColor = Color.White,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier.shadow(8.dp, spotColor = SunsetCoral, ambientColor = SunsetCoral, shape = RoundedCornerShape(percent = 50))
            ) {
                Icon(Icons.Default.Place, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Check-in", fontWeight = FontWeight.Bold)
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Thẻ Hero (Lời chào & Golden Hour)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF6B4A), // Sunset Coral
                                    Color(0xFFFFC24B), // Mango Gold
                                    Color(0xFF14B8A6)  // Lagoon Teal
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Text(
                            text = "Khám phá thế giới",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Săn lùng khoảnh khắc Golden Hour",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Giờ vàng hôm nay: 17:15 - 18:30",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 2. Ô Nhập Từ Khóa Tìm Kiếm (Search Field)
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onEvent(PlaceUiEvent.OnSearchQueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm địa điểm, khách sạn, nhà hàng, cafe...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search Icon")
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onEvent(PlaceUiEvent.OnSearchQueryChanged("")) }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // 2. Thanh Danh Mục Địa Điểm (Category Chips)
            CategoryChipRow(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { category ->
                    onEvent(PlaceUiEvent.OnCategorySelected(category))
                }
            )

            // 3. Thanh Sắp Xếp & Bộ Lọc Nâng Cao (Sort & Filter)
            FilterAndSortBar(
                currentSortType = uiState.sortType,
                minRating = uiState.minRating,
                maxPriceLevel = uiState.maxPriceLevel,
                onSortTypeChanged = { sortType ->
                    onEvent(PlaceUiEvent.OnSortTypeChanged(sortType))
                },
                onFilterRatingChanged = { rating ->
                    onEvent(PlaceUiEvent.OnFilterRatingChanged(rating))
                },
                onFilterPriceChanged = { price ->
                    onEvent(PlaceUiEvent.OnFilterPriceChanged(price))
                },
                onClearFilters = {
                    onEvent(PlaceUiEvent.OnClearFilters)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Nội dung chính xử lý 4 Trạng thái (Loading, Success, Empty, Error)
            Crossfade(
                targetState = uiState.status,
                label = "ScreenStatusTransition",
                modifier = Modifier.weight(1f)
            ) { status ->
                when (status) {
                    is PlaceScreenStatus.Loading -> {
                        LoadingStateView()
                    }

                    is PlaceScreenStatus.Empty -> {
                        EmptyStateView(
                            onClearFilters = { onEvent(PlaceUiEvent.OnClearFilters) }
                        )
                    }

                    is PlaceScreenStatus.Error -> {
                        ErrorStateView(
                            errorMessage = status.message,
                            onRetry = { onEvent(PlaceUiEvent.OnRetry) }
                        )
                    }

                    is PlaceScreenStatus.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = status.places.chunked(2),
                                key = { chunk -> chunk.first().placeId }
                            ) { rowPlaces ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    for (place in rowPlaces) {
                                        val isFav = uiState.favoritePlaceIds.contains(place.placeId)
                                        Box(modifier = Modifier.weight(1f)) {
                                            PlaceCard(
                                                place = place,
                                                isFavorite = isFav,
                                                onToggleFavorite = {
                                                    onEvent(PlaceUiEvent.OnToggleFavorite(place.placeId))
                                                },
                                                onClick = {
                                                    onEvent(PlaceUiEvent.OnPlaceSelected(place))
                                                }
                                            )
                                        }
                                    }
                                    // Empty box if chunk size is 1 to maintain grid structure
                                    if (rowPlaces.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. BottomSheet Chi Tiết khi chọn một Địa điểm
        uiState.selectedPlace?.let { selectedPlace ->
            val isFav = uiState.favoritePlaceIds.contains(selectedPlace.placeId)
            PlaceDetailBottomSheet(
                place = selectedPlace,
                isFavorite = isFav,
                onToggleFavorite = {
                    onEvent(PlaceUiEvent.OnToggleFavorite(selectedPlace.placeId))
                },
                onDismissRequest = {
                    onEvent(PlaceUiEvent.OnPlaceSelected(null))
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// SCREEN PREVIEWS (LIGHT & DARK MODE)
// ---------------------------------------------------------------------------

private val mockPlaces = listOf(
    PlaceItem(
        placeId = "1",
        name = "Bưu điện Trung tâm Sài Gòn",
        rating = 4.7,
        priceLevel = 1,
        vicinity = "2 Công xã Paris, Bến Nghé, Quận 1",
        types = listOf("tourist_attraction")
    ).apply { distanceInKm = 0.8 },
    PlaceItem(
        placeId = "2",
        name = "Nhà hát Thành phố Hồ Chí Minh",
        rating = 4.6,
        priceLevel = 2,
        vicinity = "7 Công Trường Lam Sơn, Bến Nghé, Quận 1",
        types = listOf("tourist_attraction")
    ).apply { distanceInKm = 1.1 },
    PlaceItem(
        placeId = "3",
        name = "Dinh Độc Lập",
        rating = 4.8,
        priceLevel = 2,
        vicinity = "135 Nam Kỳ Khởi Nghĩa, Bến Thành, Quận 1",
        types = listOf("tourist_attraction")
    ).apply { distanceInKm = 1.5 }
)

@Preview(name = "Discovery Screen - Success (Light Mode)", showBackground = true)
@Composable
fun PlaceDiscoveryScreenSuccessLightPreview() {
    TravelAppTheme(darkTheme = false) {
        PlaceDiscoveryScreen(
            uiState = PlaceUiState(
                searchQuery = "Sài Gòn",
                status = PlaceScreenStatus.Success(mockPlaces),
                favoritePlaceIds = setOf("1")
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Discovery Screen - Success (Dark Mode)", showBackground = true)
@Composable
fun PlaceDiscoveryScreenSuccessDarkPreview() {
    TravelAppTheme(darkTheme = true) {
        PlaceDiscoveryScreen(
            uiState = PlaceUiState(
                searchQuery = "Sài Gòn",
                status = PlaceScreenStatus.Success(mockPlaces),
                favoritePlaceIds = setOf("1")
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Discovery Screen - Loading State", showBackground = true)
@Composable
fun PlaceDiscoveryScreenLoadingPreview() {
    TravelAppTheme {
        PlaceDiscoveryScreen(
            uiState = PlaceUiState(
                status = PlaceScreenStatus.Loading
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Discovery Screen - Empty State", showBackground = true)
@Composable
fun PlaceDiscoveryScreenEmptyPreview() {
    TravelAppTheme {
        PlaceDiscoveryScreen(
            uiState = PlaceUiState(
                searchQuery = "Địa điểm không tồn tại",
                status = PlaceScreenStatus.Empty
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Discovery Screen - Error State", showBackground = true)
@Composable
fun PlaceDiscoveryScreenErrorPreview() {
    TravelAppTheme {
        PlaceDiscoveryScreen(
            uiState = PlaceUiState(
                status = PlaceScreenStatus.Error("Không thể kết nối đến máy chủ API Google Places.")
            ),
            onEvent = {}
        )
    }
}

package android_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import android_app.ui.theme.Lavender
import android_app.ui.theme.LavenderSoft
import android_app.ui.theme.MangoGold
import android_app.ui.theme.GoldSoft
import android_app.ui.theme.SunsetCoral
import android_app.ui.theme.LagoonTeal
import android_app.ui.theme.NightInk

// Danh mục địa điểm phổ biến
data class PlaceCategory(
    val id: String?,
    val label: String,
    val icon: ImageVector
)

val placeCategories = listOf(
    PlaceCategory(id = null, label = "Tất cả", Icons.Default.Place),
    PlaceCategory(id = "tourist_attraction", label = "Tham quan", Icons.Default.Star),
    PlaceCategory(id = "lodging", label = "Khách sạn", Icons.Default.Home),
    PlaceCategory(id = "restaurant", label = "Nhà hàng", Icons.Default.ThumbUp),
    PlaceCategory(id = "cafe", label = "Cà phê", Icons.Default.Favorite),
    PlaceCategory(id = "shopping_mall", label = "Mua sắm", Icons.Default.ShoppingCart),
    PlaceCategory(id = "amusement_park", label = "Vui chơi", Icons.Default.PlayArrow)
)

/**
 * Thanh cuộn chọn Danh mục Địa điểm (Category Filter Chips)
 */
@Composable
fun CategoryChipRow(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(placeCategories) { category ->
            FilterChip(
                selected = selectedCategory == category.id,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.label) },
                leadingIcon = {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.label,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

/**
 * Thanh Bộ Lọc Nâng Cao & Tùy Chọn Sắp Xếp
 */
@Composable
fun FilterAndSortBar(
    currentSortType: PlaceRepository.SortType,
    minRating: Double,
    maxPriceLevel: Int,
    onSortTypeChanged: (PlaceRepository.SortType) -> Unit,
    onFilterRatingChanged: (Double) -> Unit,
    onFilterPriceChanged: (Int) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AssistChip(
                onClick = { showSortMenu = true },
                label = {
                    val sortText = when (currentSortType) {
                        PlaceRepository.SortType.RATING_DESC -> "Đánh giá cao nhất"
                        PlaceRepository.SortType.DISTANCE -> "Gần đây nhất"
                        PlaceRepository.SortType.PRICE_ASC -> "Giá thấp đến cao"
                    }
                    Text("Sắp xếp: $sortText")
                },
                leadingIcon = {
                    Icon(Icons.Default.Menu, contentDescription = "Sort", modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            )

            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Đánh giá cao nhất") },
                    onClick = {
                        onSortTypeChanged(PlaceRepository.SortType.RATING_DESC)
                        showSortMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Khoảng cách gần nhất") },
                    onClick = {
                        onSortTypeChanged(PlaceRepository.SortType.DISTANCE)
                        showSortMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Place, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Giá cả hợp lý nhất") },
                    onClick = {
                        onSortTypeChanged(PlaceRepository.SortType.PRICE_ASC)
                        showSortMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) }
                )
            }
        }

        FilterChip(
            selected = minRating > 0 || maxPriceLevel < 4,
            onClick = { showFilterDialog = true },
            label = { Text("Bộ lọc") },
            leadingIcon = { Icon(Icons.Default.List, contentDescription = "Filter", modifier = Modifier.size(18.dp)) }
        )
    }

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Bộ Lọc Địa Điểm", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Đánh giá tối thiểu: ${if (minRating > 0) "$minRating★" else "Tất cả"}")
                    Slider(
                        value = minRating.toFloat(),
                        onValueChange = { onFilterRatingChanged(it.toDouble()) },
                        valueRange = 0f..5f,
                        steps = 9
                    )

                    Text("Mức giá tối đa: ${"$".repeat(maxPriceLevel.coerceAtLeast(1))}")
                    Slider(
                        value = maxPriceLevel.toFloat(),
                        onValueChange = { onFilterPriceChanged(it.toInt()) },
                        valueRange = 1f..4f,
                        steps = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showFilterDialog = false }) {
                    Text("Áp dụng")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onClearFilters()
                    showFilterDialog = false
                }) {
                    Text("Đặt lại")
                }
            }
        )
    }
}

/**
 * Item Card hiển thị từng Địa điểm trong Danh sách
 */
@Composable
fun PlaceCard(
    place: PlaceItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                if (place.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = place.imageUrl,
                        contentDescription = place.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Google không có ảnh cho địa điểm này
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.7f), CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) SunsetCoral else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (place.rating > 4.5) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(LavenderSoft, RoundedCornerShape(percent = 50))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Lavender, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Góc check-in đẹp", color = Lavender, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NightInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!place.vicinity.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = LagoonTeal
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = place.vicinity,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (place.openingHours.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = LagoonTeal
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = place.openingHours,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Chỉ hiện rating khi Google có đánh giá
                    if (place.rating > 0) {
                        Row(
                            modifier = Modifier
                                .background(GoldSoft, RoundedCornerShape(percent = 50))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                modifier = Modifier.size(14.dp),
                                tint = MangoGold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "%.1f".format(place.rating),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MangoGold
                            )
                        }
                    }

                    if (place.priceLevel > 0) {
                        Text(
                            text = "$".repeat(place.priceLevel),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (place.distanceInKm > 0) {
                        Text(
                            text = "• ${place.distanceInKm} km",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = LagoonTeal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingStateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(strokeWidth = 4.dp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Đang tìm kiếm địa điểm...",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun EmptyStateView(
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Không tìm thấy địa điểm nào",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onClearFilters) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Đặt lại bộ lọc")
        }
    }
}

@Composable
fun ErrorStateView(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Thử lại")
        }
    }
}

/**
 * Modal BottomSheet Chi Tiết Địa Điểm
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailBottomSheet(
    place: PlaceItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = place.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (!place.vicinity.isNullOrBlank()) {
                        Text(
                            text = place.vicinity,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) SunsetCoral else MaterialTheme.colorScheme.outline
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                DetailInfoItem(
                    icon = Icons.Default.Star,
                    title = "Đánh giá",
                    value = if (place.rating > 0) "%.1f / 5.0".format(place.rating) else "Chưa rõ",
                    tint = MangoGold
                )
                DetailInfoItem(
                    icon = Icons.Default.ShoppingCart,
                    title = "Mức giá",
                    value = if (place.priceLevel > 0) "$".repeat(place.priceLevel) else "Chưa rõ"
                )
                DetailInfoItem(
                    icon = Icons.Default.Place,
                    title = "Khoảng cách",
                    value = if (place.distanceInKm > 0) "${place.distanceInKm} km" else "Chưa rõ"
                )
            }

            if (place.openingHours.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = LagoonTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = place.openingHours,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Đóng")
                }
                Button(
                    onClick = { /* Tạo chuyến đi */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tạo chuyến đi")
                }
            }
        }
    }
}

@Composable
private fun DetailInfoItem(
    icon: ImageVector,
    title: String,
    value: String,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
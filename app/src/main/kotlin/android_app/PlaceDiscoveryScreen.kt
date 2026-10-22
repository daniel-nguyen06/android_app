package android_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android_app.ui.theme.TravelAppTheme
import coil.compose.SubcomposeAsyncImage

// ---------------------------------------------------------------------------
// BẢNG MÀU & CHỮ (phong cách tối giản, biên tập)
// ---------------------------------------------------------------------------
private val Paper = Color(0xFFFFFFFF)
private val Ink = Color(0xFF1A1A1A)
private val Mute = Color(0xFF6B6B6B)
private val Hairline = Color(0xFFE4E1DA)
private val Stone = Color(0xFFEFEDE8)
private val Green = Color(0xFF006039)
private val Gold = Color(0xFFA37E2C)
private val ErrorRed = Color(0xFFB3261E)

private val LabelStyle = TextStyle(
    fontSize = 11.sp,
    letterSpacing = 2.sp,
    fontWeight = FontWeight.Medium
)

private fun placeKindLabel(types: List<String>): String {
    fun has(prefix: String) = types.any { it.startsWith(prefix) }
    return when {
        CITY_PLACE_TYPE in types -> "ĐIỂM ĐẾN"
        has("catering.cafe") -> "CÀ PHÊ"
        has("catering") -> "ẨM THỰC"
        has("accommodation") -> "LƯU TRÚ"
        has("beach") -> "BÃI BIỂN"
        has("leisure") -> "CÔNG VIÊN"
        has("commercial") -> "MUA SẮM"
        has("entertainment") -> "GIẢI TRÍ"
        has("tourism") -> "THAM QUAN"
        else -> "ĐỊA ĐIỂM"
    }
}

// ---------------------------------------------------------------------------
// ROUTE + MÀN HÌNH
// ---------------------------------------------------------------------------
@Composable
fun PlaceDiscoveryRoute(
    viewModel: PlaceViewModel,
    onCreateTrip: (PlaceItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    PlaceDiscoveryScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onCreateTrip = onCreateTrip,
        onRefreshNearby = viewModel::loadNearby,
        modifier = modifier
    )
}

@Composable
fun PlaceDiscoveryScreen(
    uiState: PlaceUiState,
    onEvent: (PlaceUiEvent) -> Unit,
    onCreateTrip: (PlaceItem) -> Unit = {},
    onRefreshNearby: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val searching = uiState.searchQuery.isNotBlank()

    Scaffold(containerColor = Paper, modifier = modifier) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // --- Tiêu đề: theo khu vực thật của người dùng hoặc từ khóa đang tìm ---
            item(key = "header") {
                Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 8.dp)) {
                    Text(
                        text = if (searching) "KẾT QUẢ TÌM KIẾM" else "GẦN BẠN",
                        style = LabelStyle,
                        color = Gold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searching) uiState.searchQuery.trim()
                        else uiState.areaName?.let { "Quanh $it" } ?: "Quanh bạn",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Light,
                        fontSize = 38.sp,
                        lineHeight = 46.sp,
                        color = Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // --- Ô tìm kiếm (chỉ có đường kẻ dưới) ---
            item(key = "search") {
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { onEvent(PlaceUiEvent.OnSearchQueryChanged(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    placeholder = {
                        Text("Tìm điểm đến, khách sạn, quán cà phê…", color = Mute, fontSize = 16.sp)
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onEvent(PlaceUiEvent.OnSearchQueryChanged("")) }) {
                                Icon(Icons.Default.Clear, contentDescription = "Xóa", tint = Mute)
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 18.sp, fontFamily = FontFamily.Serif),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Ink,
                        unfocusedIndicatorColor = Hairline,
                        cursorColor = Green,
                        focusedTextColor = Ink,
                        unfocusedTextColor = Ink
                    )
                )
            }

            if (searching) {
                // --- Danh mục dạng chữ (chỉ hiện khi đang tìm kiếm) ---
                item(key = "categories") {
                    LazyRow(
                        modifier = Modifier.padding(top = 16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        items(placeCategories) { category ->
                            val selected = uiState.selectedCategory == category.id
                            Column(
                                modifier = Modifier.clickable { onEvent(PlaceUiEvent.OnCategorySelected(category.id)) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = category.label.uppercase(),
                                    style = LabelStyle,
                                    color = if (selected) Ink else Mute
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .height(2.dp)
                                        .width(24.dp)
                                        .background(if (selected) Gold else Color.Transparent)
                                )
                            }
                        }
                    }
                }
                item(key = "gap") { Spacer(modifier = Modifier.height(24.dp)) }
                searchResultItems(uiState, onEvent)
            } else {
                item(key = "gap") { Spacer(modifier = Modifier.height(24.dp)) }
                nearbyItems(uiState, onEvent, onRefreshNearby)
            }
        }

        // --- Chi tiết địa điểm: mở khi bấm vào ảnh ---
        uiState.selectedPlace?.let { place ->
            PlaceDetailSheet(
                place = place,
                isFavorite = uiState.favoritePlaceIds.contains(place.placeId),
                onToggleFavorite = { onEvent(PlaceUiEvent.OnToggleFavorite(place.placeId)) },
                onCreateTrip = {
                    onEvent(PlaceUiEvent.OnPlaceSelected(null))
                    onCreateTrip(place)
                },
                onDismissRequest = { onEvent(PlaceUiEvent.OnPlaceSelected(null)) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// NỘI DUNG: GỢI Ý QUANH NGƯỜI DÙNG
// ---------------------------------------------------------------------------
private fun LazyListScope.nearbyItems(
    uiState: PlaceUiState,
    onEvent: (PlaceUiEvent) -> Unit,
    onRefresh: () -> Unit
) {
    val sections = uiState.nearbySections

    when {
        uiState.userLat == null || uiState.userLng == null -> item(key = "no_location") {
            MessageBlock(
                title = "Chưa xác định được vị trí",
                body = "Hãy bật định vị và cấp quyền vị trí cho ứng dụng để xem các địa điểm quanh bạn. " +
                        "Bạn vẫn có thể nhập tên một điểm đến ở trên."
            )
        }

        sections.isEmpty() && uiState.nearbyLoading -> item(key = "nearby_loading") {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Green,
                    trackColor = Hairline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("ĐANG TÌM ĐỊA ĐIỂM QUANH BẠN", style = LabelStyle, color = Mute)
            }
        }

        sections.isEmpty() && uiState.nearbyError != null -> item(key = "nearby_error") {
            MessageBlock(
                title = "Đã xảy ra sự cố",
                body = uiState.nearbyError,
                bodyColor = ErrorRed,
                actionLabel = "THỬ LẠI",
                onAction = onRefresh
            )
        }

        sections.isEmpty() -> item(key = "nearby_empty") {
            MessageBlock(
                title = "Chưa có địa điểm quanh bạn",
                body = "Không tìm thấy địa điểm nào gần vị trí hiện tại.",
                actionLabel = "TẢI LẠI",
                onAction = onRefresh
            )
        }

        else -> {
            // Địa điểm gần nhất: ảnh lớn tràn màn hình
            val hero = sections.first().places.first()
            item(key = "hero_${hero.placeId}") {
                HeroPlaceCard(
                    place = hero,
                    isFavorite = uiState.favoritePlaceIds.contains(hero.placeId),
                    onToggleFavorite = { onEvent(PlaceUiEvent.OnToggleFavorite(hero.placeId)) },
                    onClick = { onEvent(PlaceUiEvent.OnPlaceSelected(hero)) }
                )
                Spacer(modifier = Modifier.height(40.dp))
            }

            sections.forEachIndexed { index, section ->
                val list = if (index == 0) section.places.drop(1) else section.places
                if (list.isNotEmpty()) {
                    item(key = "section_${section.categoryId}") {
                        NearbySectionRow(
                            section = section,
                            places = list,
                            favorites = uiState.favoritePlaceIds,
                            onEvent = onEvent
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// NỘI DUNG: KẾT QUẢ TÌM KIẾM
// ---------------------------------------------------------------------------
private fun LazyListScope.searchResultItems(
    uiState: PlaceUiState,
    onEvent: (PlaceUiEvent) -> Unit
) {
    when (val status = uiState.status) {
        is PlaceScreenStatus.Idle -> {}

        is PlaceScreenStatus.Loading -> item(key = "loading") {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Green,
                    trackColor = Hairline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("ĐANG TÌM KIẾM", style = LabelStyle, color = Mute)
            }
        }

        is PlaceScreenStatus.Empty -> item(key = "empty") {
            MessageBlock(
                title = "Không tìm thấy kết quả",
                body = "Hãy thử một từ khóa hoặc khu vực khác.",
                actionLabel = "ĐẶT LẠI",
                onAction = { onEvent(PlaceUiEvent.OnClearFilters) }
            )
        }

        is PlaceScreenStatus.Error -> item(key = "error") {
            MessageBlock(
                title = "Đã xảy ra sự cố",
                body = status.message,
                bodyColor = ErrorRed,
                actionLabel = "THỬ LẠI",
                onAction = { onEvent(PlaceUiEvent.OnRetry) }
            )
        }

        is PlaceScreenStatus.Success -> {
            val pinned = status.places.filter { CITY_PLACE_TYPE in it.types }
            val rest = status.places
                .filterNot { CITY_PLACE_TYPE in it.types }
                .distinctBy { it.placeId }

            item(key = "count") {
                Text(
                    text = "${status.places.size} ĐỊA ĐIỂM",
                    style = LabelStyle,
                    color = Mute,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                )
            }

            items(pinned, key = { "pin_${it.placeId}" }) { place ->
                HeroPlaceCard(
                    place = place,
                    isFavorite = uiState.favoritePlaceIds.contains(place.placeId),
                    onToggleFavorite = { onEvent(PlaceUiEvent.OnToggleFavorite(place.placeId)) },
                    onClick = { onEvent(PlaceUiEvent.OnPlaceSelected(place)) }
                )
                Spacer(modifier = Modifier.height(40.dp))
            }

            items(rest, key = { it.placeId }) { place ->
                EditorialPlaceCard(
                    place = place,
                    isFavorite = uiState.favoritePlaceIds.contains(place.placeId),
                    onToggleFavorite = { onEvent(PlaceUiEvent.OnToggleFavorite(place.placeId)) },
                    onClick = { onEvent(PlaceUiEvent.OnPlaceSelected(place)) }
                )
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// THÀNH PHẦN GIAO DIỆN
// ---------------------------------------------------------------------------

@Composable
private fun MessageBlock(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    bodyColor: Color = Mute,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(modifier = modifier.padding(horizontal = 24.dp, vertical = 24.dp)) {
        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Light,
            fontSize = 24.sp,
            color = Ink
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = body, fontSize = 14.sp, lineHeight = 20.sp, color = bodyColor)
        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = actionLabel,
                style = LabelStyle,
                color = Green,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}

/** Ảnh địa điểm; không có ảnh thì hiện nền đá với chữ cái đầu */
@Composable
private fun PlaceImage(place: PlaceItem, modifier: Modifier = Modifier) {
    @Composable
    fun Fallback(m: Modifier) {
        Box(modifier = m.background(Stone), contentAlignment = Alignment.Center) {
            Text(
                text = place.name.take(1).uppercase(),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                fontSize = 88.sp,
                color = Color(0xFFC9C5BB)
            )
        }
    }

    if (place.imageUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            model = place.imageUrl,
            contentDescription = place.name,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = { Fallback(Modifier.fillMaxSize()) },
            error = { Fallback(Modifier.fillMaxSize()) }
        )
    } else {
        Fallback(modifier)
    }
}

@Composable
private fun FavoriteButton(
    isFavorite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier
            .padding(12.dp)
            .size(40.dp)
            .background(Color.Black.copy(alpha = 0.25f), CircleShape)
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = "Yêu thích",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Lớp phủ tối dần ở nửa dưới ảnh để chữ trắng dễ đọc */
@Composable
private fun BoxScope.BottomScrim() {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(0.55f)
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                )
            )
    )
}

/** Thẻ lớn: ảnh tràn màn hình, chữ trắng đè lên */
@Composable
private fun HeroPlaceCard(
    place: PlaceItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp)
            .clickable { onClick() }
    ) {
        PlaceImage(place, Modifier.fillMaxSize())
        BottomScrim()
        FavoriteButton(isFavorite, onToggleFavorite, Modifier.align(Alignment.TopEnd))

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            val meta = buildString {
                append(placeKindLabel(place.types))
                if (place.distanceInKm > 0) append("  ·  ${place.distanceInKm} KM")
            }
            Text(meta, style = LabelStyle, color = Color.White.copy(alpha = 0.85f))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = place.name,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                fontSize = 40.sp,
                lineHeight = 46.sp,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!place.vicinity.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = place.vicinity,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Một nhóm địa điểm gần bạn: tiêu đề + hàng ảnh lớn cuộn ngang */
@Composable
private fun NearbySectionRow(
    section: NearbySection,
    places: List<PlaceItem>,
    favorites: Set<String>,
    onEvent: (PlaceUiEvent) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 40.dp)) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("${places.size} ĐỊA ĐIỂM GẦN BẠN", style = LabelStyle, color = Gold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = section.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                fontSize = 28.sp,
                color = Ink
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(places, key = { it.placeId }) { place ->
                CarouselPlaceCard(
                    place = place,
                    isFavorite = favorites.contains(place.placeId),
                    onToggleFavorite = { onEvent(PlaceUiEvent.OnToggleFavorite(place.placeId)) },
                    onClick = { onEvent(PlaceUiEvent.OnPlaceSelected(place)) }
                )
            }
        }
    }
}

@Composable
private fun CarouselPlaceCard(
    place: PlaceItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .aspectRatio(4f / 5f)
            .clickable { onClick() }
    ) {
        PlaceImage(place, Modifier.fillMaxSize())
        BottomScrim()
        FavoriteButton(isFavorite, onToggleFavorite, Modifier.align(Alignment.TopEnd))

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            if (place.distanceInKm > 0) {
                Text("${place.distanceInKm} KM", style = LabelStyle, color = Color.White.copy(alpha = 0.85f))
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = place.name,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Light,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Thẻ địa điểm trong kết quả tìm kiếm: ảnh lớn, thông tin tối giản bên dưới */
@Composable
private fun EditorialPlaceCard(
    place: PlaceItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(5f / 6f)
                .clickable { onClick() }
        ) {
            PlaceImage(place, Modifier.fillMaxSize())
            FavoriteButton(isFavorite, onToggleFavorite, Modifier.align(Alignment.TopEnd))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(placeKindLabel(place.types), style = LabelStyle, color = Gold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = place.name,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Light,
            fontSize = 26.sp,
            lineHeight = 32.sp,
            color = Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!place.vicinity.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = place.vicinity,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Mute,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hairline))
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("KHÁM PHÁ", style = LabelStyle, color = Green)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.size(14.dp)
                )
            }
            if (place.distanceInKm > 0) {
                Text("${place.distanceInKm} KM", style = LabelStyle, color = Mute)
            }
        }
    }
}

/** Chi tiết địa điểm + nút tạo chuyến đi */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailSheet(
    place: PlaceItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCreateTrip: () -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Paper,
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                PlaceImage(place, Modifier.fillMaxSize())
                FavoriteButton(isFavorite, onToggleFavorite, Modifier.align(Alignment.TopEnd))
            }

            Column(modifier = Modifier.padding(24.dp)) {
                Text(placeKindLabel(place.types), style = LabelStyle, color = Gold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = place.name,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Light,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    color = Ink
                )
                if (!place.vicinity.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = place.vicinity, fontSize = 14.sp, lineHeight = 20.sp, color = Mute)
                }

                // Chỉ hiện những thông tin có dữ liệu thật
                val facts = buildList {
                    if (place.rating > 0) add("ĐÁNH GIÁ" to "%.1f / 5".format(place.rating))
                    if (place.priceLevel > 0) add("MỨC GIÁ" to "$".repeat(place.priceLevel))
                    if (place.distanceInKm > 0) add("KHOẢNG CÁCH" to "${place.distanceInKm} km")
                }
                if (facts.isNotEmpty() || place.openingHours.isNotBlank()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hairline))
                    Spacer(modifier = Modifier.height(20.dp))
                }
                if (facts.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        facts.forEach { (label, value) ->
                            Column {
                                Text(label, style = LabelStyle, color = Mute)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = value,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 20.sp,
                                    color = Ink
                                )
                            }
                        }
                    }
                }
                if (place.openingHours.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("GIỜ MỞ CỬA", style = LabelStyle, color = Mute)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = place.openingHours, fontSize = 14.sp, lineHeight = 20.sp, color = Ink)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onCreateTrip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White)
                ) {
                    Text("TẠO CHUYẾN ĐI TỚI ĐÂY", style = LabelStyle)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ĐÓNG",
                    style = LabelStyle,
                    color = Mute,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismissRequest() }
                        .padding(vertical = 12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PREVIEWS
// ---------------------------------------------------------------------------

private val previewPlaces = listOf(
    PlaceItem(
        placeId = "1",
        name = "Bưu điện Trung tâm Sài Gòn",
        vicinity = "2 Công xã Paris, Bến Nghé, Quận 1",
        types = listOf("tourism.sights")
    ).apply { distanceInKm = 0.8 },
    PlaceItem(
        placeId = "2",
        name = "Nhà hát Thành phố Hồ Chí Minh",
        vicinity = "7 Công Trường Lam Sơn, Bến Nghé, Quận 1",
        types = listOf("tourism.sights")
    ).apply { distanceInKm = 1.1 }
)

@Preview(name = "Discovery - Gần bạn", showBackground = true)
@Composable
fun PlaceDiscoveryScreenNearbyPreview() {
    TravelAppTheme(darkTheme = false) {
        PlaceDiscoveryScreen(
            uiState = PlaceUiState(
                userLat = 10.77,
                userLng = 106.70,
                areaName = "Hồ Chí Minh",
                nearbySections = listOf(NearbySection("tourism.sights", "Tham quan", previewPlaces))
            ),
            onEvent = {}
        )
    }
}

@Preview(name = "Discovery - Chưa có vị trí", showBackground = true)
@Composable
fun PlaceDiscoveryScreenNoLocationPreview() {
    TravelAppTheme(darkTheme = false) {
        PlaceDiscoveryScreen(uiState = PlaceUiState(), onEvent = {})
    }
}
package android_app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android_app.ui.theme.TravelAppTheme
import com.travel.android_app.BuildConfig

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Các Tab điều hướng của ứng dụng
 */
enum class TravelTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    EXPLORE("Khám phá", Icons.Filled.Explore, Icons.Outlined.Explore),
    FAVORITES("Yêu thích", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    TRIPS("Chuyến đi", Icons.Filled.Luggage, Icons.Outlined.Luggage),
    MENU("Menu", Icons.Filled.Menu, Icons.Outlined.Menu)
}

enum class NavigationRoute {
    HOME,
    LOGIN,
    REGISTER,
    FIND_LOCATION,
    CREATE_TRIP,
    CREATE_ITINERARY,
    VIEW_ITINERARY
}

/**
 * Shell chính của ứng dụng, chứa Bottom Navigation Bar.
 */
@Composable
fun MainTravelAppScreen(
    placeViewModel: PlaceViewModel,
    tripViewModel: TripViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(TravelTab.EXPLORE) }
    var currentRoute by remember { mutableStateOf(NavigationRoute.LOGIN) }

    var fullName by remember { mutableStateOf<String?>(null) }
    var isLoadingProfile by remember { mutableStateOf(false) }
    var profileError by remember { mutableStateOf<String?>(null) }

    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }

    fun loadUserProfile() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            currentRoute = NavigationRoute.LOGIN
            return
        }

        isLoadingProfile = true
        profileError = null

        firestore.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("fullName")

                if (!name.isNullOrBlank()) {
                    fullName = name
                    isLoadingProfile = false
                    currentRoute = NavigationRoute.HOME
                } else {
                    isLoadingProfile = false
                    profileError = "Không tìm thấy fullName trong hồ sơ người dùng."
                }
            }
            .addOnFailureListener { exception ->
                isLoadingProfile = false
                profileError = exception.localizedMessage
                    ?: "Không tải được hồ sơ người dùng."
            }
    }

    if (currentRoute != NavigationRoute.HOME) {
        when (currentRoute) {
            NavigationRoute.LOGIN -> {
                android_app.ui.screens.LoginScreen(
                    onNavigateToHome = { loadUserProfile() },
                    onNavigateToRegister = { currentRoute = NavigationRoute.REGISTER }
                )
            }
            NavigationRoute.REGISTER -> {
                android_app.ui.screens.RegisterScreen(
                    onNavigateToLogin = { currentRoute = NavigationRoute.LOGIN }
                )
            }
            NavigationRoute.FIND_LOCATION -> {
                android_app.ui.screens.FindLocationScreen(
                    viewModel = placeViewModel,
                    onBack = { currentRoute = NavigationRoute.HOME }
                )
            }
            NavigationRoute.CREATE_TRIP -> {
                android_app.ui.screens.CreateTripScreen(
                    viewModel = tripViewModel,
                    onBack = { currentRoute = NavigationRoute.HOME }
                )
            }
            NavigationRoute.CREATE_ITINERARY -> {
                android_app.ui.screens.CreateItineraryScreen(onBack = { currentRoute = NavigationRoute.HOME })
            }
            NavigationRoute.VIEW_ITINERARY -> {
                android_app.ui.screens.ViewItineraryScreen(
                    viewModel = tripViewModel,
                    onBack = { currentRoute = NavigationRoute.HOME }
                )
            }
            else -> {}
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.height(72.dp)
            ) {
                TravelTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedTab = tab },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .padding(horizontal = 16.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                TravelTab.EXPLORE -> {
                    PlaceDiscoveryRoute(viewModel = placeViewModel)
                }
                TravelTab.FAVORITES -> {
                    FavoritesTabScreen(viewModel = placeViewModel)
                }
                TravelTab.TRIPS -> {
                    TripsTabScreen(
                        viewModel = tripViewModel,
                        onCreateTrip = { currentRoute = NavigationRoute.CREATE_TRIP }
                    )
                }
                TravelTab.MENU -> {
                    android_app.ui.screens.MenuScreen(
                        fullName = fullName.orEmpty(),
                        onNavigateToFindLocation = { currentRoute = NavigationRoute.FIND_LOCATION },
                        onNavigateToCreateTrip = { currentRoute = NavigationRoute.CREATE_TRIP },
                        onNavigateToCreateItinerary = { currentRoute = NavigationRoute.CREATE_ITINERARY },
                        onNavigateToViewItinerary = { currentRoute = NavigationRoute.VIEW_ITINERARY },
                        onLogout = {
                            fullName = null
                            profileError = null
                            selectedTab = TravelTab.EXPLORE
                            currentRoute = NavigationRoute.LOGIN
                        }
                    )
                }
            }
        }
    }
}

/**
 * Tab Địa điểm Yêu thích
 */
@Composable
fun FavoritesTabScreen(
    viewModel: PlaceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val favPlaces = uiState.favoritePlaces.values.toList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Địa Điểm Đã Lưu (${favPlaces.size})",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (favPlaces.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Chưa có địa điểm yêu thích nào",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Nhấn biểu tượng trái tim để lưu lại địa điểm bạn muốn ghé thăm.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = favPlaces,
                    key = { it.placeId }
                ) { place ->
                    PlaceCard(
                        place = place,
                        isFavorite = true,
                        onToggleFavorite = { viewModel.onEvent(PlaceUiEvent.OnToggleFavorite(place.placeId)) },
                        onClick = { viewModel.onEvent(PlaceUiEvent.OnPlaceSelected(place)) }
                    )
                }
            }
        }

        uiState.selectedPlace?.let { selected ->
            PlaceDetailBottomSheet(
                place = selected,
                isFavorite = uiState.favoritePlaceIds.contains(selected.placeId),
                onToggleFavorite = { viewModel.onEvent(PlaceUiEvent.OnToggleFavorite(selected.placeId)) },
                onDismissRequest = { viewModel.onEvent(PlaceUiEvent.OnPlaceSelected(null)) }
            )
        }
    }
}

/**
 * Tab Quản lý Chuyến đi (đọc dữ liệu thật từ TripViewModel)
 */
@Composable
fun TripsTabScreen(
    viewModel: TripViewModel,
    onCreateTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Chuyến đi của tôi (${uiState.trips.size})",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = onCreateTrip) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tạo mới")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.trips.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Luggage,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Bạn chưa có chuyến đi nào.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.trips, key = { it.tripId }) { trip ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                trip.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(trip.destination, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${trip.startDate} - ${trip.endDate} · ${trip.totalDays} ngày",
                                style = MaterialTheme.typography.bodySmall
                            )
                            trip.budget?.let {
                                Text(
                                    "Dự kiến: ${it.totalEstimated}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PREVIEWS
// ---------------------------------------------------------------------------

@Preview(name = "Main Screen - Light Theme", showBackground = true)
@Composable
fun MainTravelAppScreenLightPreview() {
    val placeVM = PlaceViewModel(
        PlaceRepository(aiParser = AiQueryParser(geminiApiKey = BuildConfig.GEMINI_API_KEY))
    )
    val tripVM = TripViewModel(
        TripRepository(budgetService = GeminiBudgetService(geminiApiKey = BuildConfig.GEMINI_API_KEY))
    )
    TravelAppTheme(darkTheme = false) {
        MainTravelAppScreen(placeViewModel = placeVM, tripViewModel = tripVM)
    }
}

@Preview(name = "Main Screen - Dark Theme", showBackground = true)
@Composable
fun MainTravelAppScreenDarkPreview() {
    val placeVM = PlaceViewModel(
        PlaceRepository(aiParser = AiQueryParser(geminiApiKey = BuildConfig.GEMINI_API_KEY))
    )
    val tripVM = TripViewModel(
        TripRepository(budgetService = GeminiBudgetService(geminiApiKey = BuildConfig.GEMINI_API_KEY))
    )
    TravelAppTheme(darkTheme = true) {
        MainTravelAppScreen(placeViewModel = placeVM, tripViewModel = tripVM)
    }
}
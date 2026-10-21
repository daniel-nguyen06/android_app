package android_app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import android_app.ui.theme.TravelAppTheme
import com.travel.android_app.BuildConfig
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var placeViewModel: PlaceViewModel
    private lateinit var locationTracker: LocationTracker

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Chỉ cần một trong hai quyền (chính xác hoặc xấp xỉ) là đủ
        if (permissions.values.any { it }) fetchUserLocation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val aiParser = AiQueryParser(geminiApiKey = BuildConfig.GEMINI_API_KEY)
        val budgetService = GeminiBudgetService(geminiApiKey = BuildConfig.GEMINI_API_KEY)
        val tripRepository = TripRepository(budgetService = budgetService)
        val placeRepository = PlaceRepository(aiParser = aiParser)

        placeViewModel = PlaceViewModel(repository = placeRepository)
        val tripViewModel = TripViewModel(repository = tripRepository)
        locationTracker = LocationTracker(applicationContext)

        if (hasLocationPermission()) {
            fetchUserLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        setContent {
            TravelAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainTravelAppScreen(
                        placeViewModel = placeViewModel,
                        tripViewModel = tripViewModel
                    )
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun fetchUserLocation() {
        lifecycleScope.launch {
            locationTracker.getCurrentLocation()?.let { (lat, lng) ->
                placeViewModel.updateUserLocation(lat, lng)
            }
        }
    }
}
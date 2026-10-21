package android_app

import com.travel.android_app.BuildConfig
import kotlinx.coroutines.CancellationException
import java.util.Locale

class TravelBackendService(
    private val placesApi: PlacesApiService = PlacesApiService.create(),
    private val weatherApi: WeatherApiService = WeatherApiService.create(),
    private val weatherKey: String = BuildConfig.WEATHER_API_KEY
) {

    private companion object {
        const val NEARBY_RADIUS_METERS = 5000.0
    }

    suspend fun searchAndFilterPlaces(
        query: String,
        category: String? = null,
        minRating: Double = 0.0,
        userLat: Double? = null,
        userLng: Double? = null
    ): List<PlaceItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            val bias = if (userLat != null && userLng != null) {
                LocationBias(Circle(LatLngLiteral(userLat, userLng), NEARBY_RADIUS_METERS))
            } else null

            val response = placesApi.searchText(
                TextSearchRequest(
                    textQuery = trimmed,
                    languageCode = Locale.getDefault().language,
                    includedType = category,
                    locationBias = bias
                )
            )

            response.places.orEmpty()
                .mapNotNull { it.toPlaceItem(userLat, userLng) }
                .filter { it.rating >= minRating }
                .sortedBy { it.distanceInKm }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPlaceWeather(lat: Double, lng: Double): WeatherResponse? {
        return try {
            if (weatherKey.isNotBlank()) {
                weatherApi.getCurrentWeather(lat = lat, lon = lng, apiKey = weatherKey)
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
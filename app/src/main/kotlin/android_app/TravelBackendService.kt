package android_app

import com.travel.android_app.BuildConfig
import kotlinx.coroutines.CancellationException
import java.util.Locale

class TravelBackendService(
    private val placesApi: PlacesApiService = PlacesApiService.create(),
    private val weatherApi: WeatherApiService = WeatherApiService.create(),
    private val weatherKey: String = BuildConfig.WEATHER_API_KEY
) {

    /** Tìm địa điểm quanh một toạ độ (bán kính mét) theo danh mục Geoapify */
    suspend fun searchNearby(
        categories: String,
        lat: Double,
        lng: Double,
        radiusMeters: Int = 5000
    ): List<PlaceItem> {
        return try {
            placesApi.searchPlaces(
                categories = categories,
                filter = "circle:$lng,$lat,$radiusMeters",
                bias = "proximity:$lng,$lat",
                lang = Locale.getDefault().language
            ).features.orEmpty()
                .mapNotNull { it.toPlaceItem(lat, lng) }
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
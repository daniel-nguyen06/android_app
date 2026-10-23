package android_app

import com.google.gson.annotations.SerializedName

// --- WEATHER MODELS (OpenWeatherMap) ---
data class WeatherResponse(
        @SerializedName("weather") val weather: List<WeatherDescription> = emptyList(),
        @SerializedName("main") val main: MainWeatherData? = null,
        @SerializedName("name") val cityName: String? = null
)

data class WeatherDescription(
        @SerializedName("description") val description: String,
        @SerializedName("icon") val icon: String
)

data class MainWeatherData(
        @SerializedName("temp") val temp: Double,
        @SerializedName("humidity") val humidity: Int
)

// --- MODEL HIỂN THỊ TRÊN UI (dữ liệu lấy từ Google Maps) ---
data class PlaceItem(
        val placeId: String,
        val name: String,
        val vicinity: String? = null,
        /** 0.0 nghĩa là Google chưa có đánh giá cho địa điểm này */
        val rating: Double = 0.0,
        /** 0 nghĩa là Google không cung cấp mức giá; 1-4 tương ứng $ đến $$$$ */
        val priceLevel: Int = 0,
        val estimatedBudget: String = "",
        val openingHours: String = "",
        val description: String = "",
        val imageUrl: String = "",
        val types: List<String> = emptyList(),
        val geometry: Geometry? = null,
        var distanceInKm: Double = 0.0
)

data class Geometry(val location: LatLng? = null)
data class LatLng(val lat: Double, val lng: Double)
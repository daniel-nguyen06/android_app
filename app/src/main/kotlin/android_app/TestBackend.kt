package android_app

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("==================================================")
    println(">>> TEST AI PARSER + GEOAPIFY PLACES API...")
    println("==================================================")

    val geoapifyKey = "2578b45979c64af6a1c192e486b9181d"
    val geminiKey = "AQ.Ab8RN6KN_pdWeE38ANxu9Os1B1JmyzoNWOwU104ckgUj9KEyOw"

    val aiParser = AiQueryParser(geminiApiKey = geminiKey)
    val repository = PlaceRepository(
        apiKey = geoapifyKey,
        aiParser = aiParser
    )

    val query = "Tìm quán Cộng Coffee gần đây"
    val places = repository.fetchPlaces(
        keyword = query,
        userLat = 21.0285,
        userLng = 105.8542
    )

    println("\n>>> TỔNG SỐ ĐỊA ĐIỂM TÌM THẤY: ${places.size}")
    places.forEachIndexed { index, place ->
        println("${index + 1}. ${place.name}")
        println("   - Khoảng cách: ${place.distanceInKm} km")
        println("   - Địa chỉ: ${place.vicinity}")
        println("   - Thể loại: ${place.types}")
        println("   - Ảnh: ${place.imageUrl}")
        println("--------------------------------------------------")
    }
}
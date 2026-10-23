
package android_app

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FavoriteRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val placesCollection = firestore.collection("places")
    private val favoritesCollection = firestore.collection("favorite_places")

    private fun favoriteDocumentId(userId: String, placeId: String): String =
        "${userId}_${placeId}"

    /**
     * Lấy danh sách liên kết yêu thích, sau đó đọc dữ liệu địa điểm
     * tương ứng từ collection places.
     */
    suspend fun getFavorites(userId: String): Map<String, PlaceItem> {
        val favoriteSnapshot = favoritesCollection
            .whereEqualTo("userId", userId)
            .get()
            .awaitResult()

        val result = mutableMapOf<String, PlaceItem>()

        for (favoriteDocument in favoriteSnapshot.documents) {
            val placeId = favoriteDocument.getString("placeId")
                ?: continue

            val placeDocument = placesCollection
                .document(placeId)
                .get()
                .awaitResult()

            val place = placeDocument.toPlaceItem()
                ?: continue

            result[place.placeId] = place
        }

        return result
    }

    /**
     * Lưu/cập nhật thông tin địa điểm vào places trước,
     * sau đó tạo tài liệu yêu thích chỉ chứa 3 trường.
     */
    suspend fun addFavorite(userId: String, place: PlaceItem) {
        val placeData = mapOf(
            "placeId" to place.placeId,
            "name" to place.name,
            "vicinity" to place.vicinity,
            "rating" to place.rating,
            "priceLevel" to place.priceLevel,
            "estimatedBudget" to place.estimatedBudget,
            "openingHours" to place.openingHours,
            "description" to place.description,
            "imageUrl" to place.imageUrl,
            "types" to place.types,
            "geometry" to place.geometry?.location?.let { location ->
                mapOf(
                    "location" to mapOf(
                        "lat" to location.lat,
                        "lng" to location.lng
                    )
                )
            },
            "distanceInKm" to place.distanceInKm
        )

        // Dữ liệu địa điểm được lưu dùng chung trong places/{placeId}.
        placesCollection
            .document(place.placeId)
            .set(placeData, SetOptions.merge())
            .awaitResult()

        // favorite_places chỉ lưu userId, placeId, createdAt.
        favoritesCollection
            .document(favoriteDocumentId(userId, place.placeId))
            .set(
                mapOf(
                    "userId" to userId,
                    "placeId" to place.placeId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .awaitResult()
    }

    suspend fun removeFavorite(userId: String, placeId: String) {
        // Chỉ xóa liên kết yêu thích; không xóa địa điểm dùng chung.
        favoritesCollection
            .document(favoriteDocumentId(userId, placeId))
            .delete()
            .awaitResult()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toPlaceItem():
            PlaceItem? {
        if (!exists()) return null

        val id = getString("placeId") ?: return null
        val name = getString("name") ?: return null

        val geometryMap = get("geometry") as? Map<*, *>
        val locationMap = geometryMap?.get("location") as? Map<*, *>

        val lat = (locationMap?.get("lat") as? Number)?.toDouble()
        val lng = (locationMap?.get("lng") as? Number)?.toDouble()

        val geometry = if (lat != null && lng != null) {
            Geometry(location = LatLng(lat, lng))
        } else {
            null
        }

        return PlaceItem(
            placeId = id,
            name = name,
            vicinity = getString("vicinity"),
            rating = getDouble("rating") ?: 0.0,
            priceLevel = getLong("priceLevel")?.toInt() ?: 0,
            estimatedBudget = getString("estimatedBudget") ?: "",
            openingHours = getString("openingHours") ?: "",
            description = getString("description") ?: "",
            imageUrl = getString("imageUrl") ?: "",
            types = (get("types") as? List<*>)
                ?.filterIsInstance<String>()
                ?: emptyList(),
            geometry = geometry,
            distanceInKm = getDouble("distanceInKm") ?: 0.0
        )
    }

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }
            addOnFailureListener { error ->
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }
            addOnCanceledListener {
                if (continuation.isActive) {
                    continuation.cancel()
                }
            }
        }
}

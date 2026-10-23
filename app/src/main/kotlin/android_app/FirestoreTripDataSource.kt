package android_app

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirestoreTripDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : TripDataSource {

    private val tripsCollection
        get() = firestore.collection("trips")

    private fun currentUserId(): String? = auth.currentUser?.uid


    override suspend fun saveTrip(trip: TripPlan): Boolean {
        val uid = currentUserId()
            ?: throw IllegalStateException(
                "Người dùng chưa đăng nhập Firebase."
            )

        // Ghi trực tiếp, không đọc tài liệu trước.
        // Firestore Rules sẽ kiểm tra quyền tạo/cập nhật.
        tripsCollection
            .document(trip.tripId)
            .set(trip.toFirestoreMap(uid))
            .await()

        Log.d(
            "TripFirebase",
            "Đã lưu chuyến đi ${trip.tripId}, UID=$uid"
        )

        return true
    }

    override suspend fun getTrips(): List<TripPlan> {
        val uid = currentUserId() ?: return emptyList()

        val snapshot = tripsCollection
            .whereEqualTo("userId", uid)
            .get()
            .await()

        Log.d(
            "TripFirebase",
            "UID hiện tại: ${auth.currentUser?.uid}"
        )

        return snapshot.documents.mapNotNull { it.toTripPlan() }
            .sortedByDescending { it.createdAt }
    }

    override suspend fun getTripById(tripId: String): TripPlan? {
        val uid = currentUserId() ?: return null
        val snapshot = tripsCollection.document(tripId).get().await()

        if (!snapshot.exists() || snapshot.getString("userId") != uid) {
            return null
        }

        return snapshot.toTripPlan()
    }

    override suspend fun addStopToTrip(
        tripId: String,
        stop: TripStop
    ): Boolean {
        val trip = getTripById(tripId) ?: return false

        val newStop = stop.copy(
            stopOrder = trip.stops.size + 1
        )

        return saveTrip(
            trip.copy(stops = trip.stops + newStop)
        )
    }

    override suspend fun addExpenseToTrip(
        tripId: String,
        expense: TripExpense
    ): Boolean {
        val trip = getTripById(tripId) ?: return false

        return saveTrip(
            trip.copy(expenses = trip.expenses + expense)
        )
    }

    override suspend fun deleteTrip(tripId: String): Boolean {
        val uid = currentUserId() ?: return false
        val document = tripsCollection.document(tripId)
        val snapshot = document.get().await()

        if (!snapshot.exists() || snapshot.getString("userId") != uid) {
            return false
        }

        document.delete().await()
        return true
    }

    private fun TripPlan.toFirestoreMap(uid: String): Map<String, Any?> =
        mapOf(
            "tripId" to tripId,
            "userId" to uid,
            "title" to title,
            "destination" to destination,
            "startDate" to startDate,
            "endDate" to endDate,
            "totalDays" to totalDays,
            "stops" to stops.map { stop ->
                mapOf(
                    "stopId" to stop.stopId,
                    "placeId" to stop.placeId,
                    "name" to stop.name,
                    "address" to stop.address,
                    "lat" to stop.lat,
                    "lng" to stop.lng,
                    "imageUrl" to stop.imageUrl,
                    "stopOrder" to stop.stopOrder
                )
            },
            "budget" to budget?.let {
                mapOf(
                    "transportCost" to it.transportCost,
                    "accommodationCost" to it.accommodationCost,
                    "foodCost" to it.foodCost,
                    "entertainmentCost" to it.entertainmentCost,
                    "totalEstimated" to it.totalEstimated,
                    "summaryAdvice" to it.summaryAdvice
                )
            },
            "expenses" to expenses.map { expense ->
                mapOf(
                    "expenseId" to expense.expenseId,
                    "title" to expense.title,
                    "amount" to expense.amount,
                    "category" to expense.category.name,
                    "timestamp" to expense.timestamp
                )
            },
            "createdAt" to createdAt
        )

    private fun com.google.firebase.firestore.DocumentSnapshot.toTripPlan():
            TripPlan? {
        if (!exists()) return null

        val stopList = (get("stops") as? List<*>)
            .orEmpty()
            .mapNotNull { item ->
                val map = item.asStringKeyMap() ?: return@mapNotNull null

                TripStop(
                    stopId = map.string("stopId")
                        ?: return@mapNotNull null,
                    placeId = map.string("placeId") ?: "",
                    name = map.string("name") ?: "",
                    address = map.string("address"),
                    lat = map.double("lat"),
                    lng = map.double("lng"),
                    imageUrl = map.string("imageUrl"),
                    stopOrder = map.int("stopOrder") ?: 1
                )
            }

        val budgetMap = (get("budget") as? Map<*, *>)
            ?.asStringKeyMap()

        val budget = budgetMap?.let { map ->
            BudgetBreakdown(
                transportCost = map.string("transportCost") ?: "0đ",
                accommodationCost = map.string("accommodationCost") ?: "0đ",
                foodCost = map.string("foodCost") ?: "0đ",
                entertainmentCost = map.string("entertainmentCost") ?: "0đ",
                totalEstimated = map.string("totalEstimated") ?: "0đ",
                summaryAdvice = map.string("summaryAdvice") ?: ""
            )
        }

        val expenseList = (get("expenses") as? List<*>)
            .orEmpty()
            .mapNotNull { item ->
                val map = item.asStringKeyMap() ?: return@mapNotNull null
                val categoryName = map.string("category")
                    ?: return@mapNotNull null
                val category = runCatching {
                    ExpenseCategory.valueOf(categoryName)
                }.getOrNull() ?: ExpenseCategory.OTHER

                TripExpense(
                    expenseId = map.string("expenseId")
                        ?: return@mapNotNull null,
                    title = map.string("title") ?: "",
                    amount = map.long("amount") ?: 0L,
                    category = category,
                    timestamp = map.long("timestamp") ?: 0L
                )
            }

        return TripPlan(
            tripId = getString("tripId") ?: id,
            title = getString("title") ?: "",
            destination = getString("destination") ?: "",
            startDate = getString("startDate") ?: "",
            endDate = getString("endDate") ?: "",
            totalDays = (get("totalDays") as? Number)?.toInt() ?: 1,
            stops = stopList,
            budget = budget,
            expenses = expenseList,
            createdAt = (get("createdAt") as? Number)?.toLong() ?: 0L
        )
    }

    private fun Any?.asStringKeyMap(): Map<String, Any?>? {
        val source = this as? Map<*, *> ?: return null
        return source.entries.associate { (key, value) ->
            key.toString() to value
        }
    }

    private fun Map<String, Any?>.string(key: String): String? =
        this[key] as? String

    private fun Map<String, Any?>.double(key: String): Double? =
        (this[key] as? Number)?.toDouble()

    private fun Map<String, Any?>.int(key: String): Int? =
        (this[key] as? Number)?.toInt()

    private fun Map<String, Any?>.long(key: String): Long? =
        (this[key] as? Number)?.toLong()
}

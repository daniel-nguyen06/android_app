
package com.example.backend

import com.google.cloud.Timestamp
import com.google.cloud.firestore.Query
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TripRepository {

    private val db = FirebaseConfig.getFirestore()
    private val trips = db.collection("trips")

    // Chuyển dữ liệu ngày từ Firestore thành chuỗi ISO
    private fun dateToString(value: Any?): String {
        return when (value) {
            is Timestamp -> value.toDate().toInstant()
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toLocalDateTime()
                .toString()

            is String -> value

            else -> ""
        }
    }

    // Đọc số tiền dù Firestore lưu kiểu Integer, Long hoặc Double
    private fun numberToLong(value: Any?): Long {
        return when (value) {
            is Number -> value.toLong()
            else -> 0L
        }
    }

    // Tạo chuyến đi mới
    fun createTrip(trip: Trip): String {
        require(trip.name.isNotBlank()) {
            "Tên chuyến đi không được để trống"
        }
        require(trip.budget >= 0L) {
            "Ngân sách không được âm"
        }

        if (trip.startDate.isNotBlank() && trip.endDate.isNotBlank()) {
            require(trip.startDate.take(10) <= trip.endDate.take(10)) {
                "Ngày bắt đầu không được sau ngày kết thúc"
            }
        }

        val document = trips.document()

        val data = mapOf(
            "userId" to trip.userId,
            "name" to trip.name,
            "startDate" to trip.startDate,
            "endDate" to trip.endDate,
            "budget" to trip.budget,
            "currency" to "VND",
            "participantCount" to trip.participantCount,
            "createdAt" to Timestamp.now(),
            "totalSpent" to 0L
        )

        document.set(data).get()
        return document.id
    }

    // Lấy một chuyến đi
    fun getTrip(tripId: String): Trip? {
        val document = trips.document(tripId).get().get()

        if (!document.exists()) return null

        return Trip(
            tripId = document.id,
            userId = document.getString("userId") ?: "",
            name = document.getString("name") ?: "",
            startDate = dateToString(document.get("startDate")),
            endDate = dateToString(document.get("endDate")),
            budget = numberToLong(document.get("budget")),
            participantCount =
                numberToLong(document.get("participantCount")).toInt(),
            createdAt = dateToString(document.get("createdAt"))
        )
    }

    // Lấy các chuyến đi của người dùng
    fun getTripsByUser(userId: String): List<Trip> {
        val result = trips
            .whereEqualTo("userId", userId)
            .get()
            .get()

        return result.documents.mapNotNull { document ->
            getTrip(document.id)
        }.sortedByDescending { it.createdAt }
    }

    // Cập nhật chuyến đi
    fun updateTrip(
        tripId: String,
        name: String,
        startDate: String,
        endDate: String,
        budget: Long
    ) {
        require(name.isNotBlank()) {
            "Tên chuyến đi không được để trống"
        }
        require(budget >= 0L) {
            "Ngân sách không được âm"
        }

        if (startDate.isNotBlank() && endDate.isNotBlank()) {
            require(startDate.take(10) <= endDate.take(10)) {
                "Ngày bắt đầu không được sau ngày kết thúc"
            }
        }

        trips.document(tripId).update(
            mapOf(
                "name" to name,
                "startDate" to startDate,
                "endDate" to endDate,
                "budget" to budget
            )
        ).get()
    }

    // Xóa chuyến đi
    fun deleteTrip(tripId: String) {
        trips.document(tripId).delete().get()
    }
}

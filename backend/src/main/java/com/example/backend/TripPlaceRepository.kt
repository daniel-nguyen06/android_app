package com.example.backend

class TripPlaceRepository {

    private val db = FirebaseConfig.getFirestore()

    private val tripPlaces = db.collection("trip_places")
    private val trips = db.collection("trips")
    private val places = db.collection("places")

    // Thêm địa điểm vào chuyến đi
    fun addPlace(
        tripId: String,
        placeId: String,
        order: Int,
        plannedDate: String = "",
        note: String = ""
    ): String {

        require(order >= 1) {
            "Thứ tự địa điểm phải lớn hơn hoặc bằng 1"
        }

        // Kiểm tra chuyến đi có tồn tại không
        val tripDocument = trips
            .document(tripId)
            .get()
            .get()

        require(tripDocument.exists()) {
            "Chuyến đi không tồn tại"
        }

        // Kiểm tra địa điểm có tồn tại trong collection places không
        val placeDocument = places
            .document(placeId)
            .get()
            .get()

        require(placeDocument.exists()) {
            "Địa điểm không tồn tại"
        }

        val document = tripPlaces.document()

        val data = mapOf(
            "tripId" to tripId,
            "placeId" to placeId,
            "order" to order,
            "plannedDate" to plannedDate,
            "note" to note
        )

        document.set(data).get()

        return document.id
    }

    // Lấy danh sách địa điểm của chuyến đi
    fun getPlacesByTrip(tripId: String): List<TripPlace> {
        val result = tripPlaces
            .whereEqualTo("tripId", tripId)
            .orderBy("order")
            .get()
            .get()

        return result.documents.map { document ->
            val rawDate = document.get("plannedDate")

            val plannedDate = when (rawDate) {
                is com.google.cloud.Timestamp ->
                    rawDate.toDate().toInstant().toString()

                is String -> rawDate

                else -> ""
            }

            TripPlace(
                tripPlaceId = document.id,
                tripId = document.getString("tripId") ?: "",
                placeId = document.getString("placeId") ?: "",
                order = (document.get("order") as? Number)?.toInt() ?: 0,
                plannedDate = plannedDate,
                note = document.getString("note") ?: ""
            )
        }
    }

    // Cập nhật thứ tự, ngày dự kiến và ghi chú
    fun updateTripPlace(
        tripPlaceId: String,
        order: Int,
        plannedDate: String,
        note: String
    ) {

        require(order >= 1) {
            "Thứ tự địa điểm phải lớn hơn hoặc bằng 1"
        }

        tripPlaces.document(tripPlaceId).update(
            mapOf(
                "order" to order,
                "plannedDate" to plannedDate,
                "note" to note
            )
        ).get()
    }

    // Xóa một địa điểm khỏi chuyến đi
    fun removePlace(tripPlaceId: String) {
        tripPlaces.document(tripPlaceId).delete().get()
    }
}

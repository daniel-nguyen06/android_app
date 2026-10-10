package com.example.backend

import java.time.Instant

class TripExpenseRepository {

    private val db = FirebaseConfig.getFirestore()
    private val expenses = db.collection("trip_expenses")
    private val trips = db.collection("trips")
    private val participants = db.collection("trip_participants")

    // Thêm khoản chi mới
    fun addExpense(
        tripId: String,
        title: String,
        amount: Double,
        paidBy: String,
        participantIds: List<String>,
        splitType: String,
        tripPlaceId: String
    ): String {
        require(title.isNotBlank()) { "Nội dung chi tiêu không được để trống" }
        require(amount > 0.0) { "Số tiền phải lớn hơn 0" }
        require(participantIds.isNotEmpty()) { "Phải có ít nhất một người tham gia chi phí" }
        require(splitType in setOf("equal", "custom")) {
            "Kiểu chia chi phí không hợp lệ"
        }

        val tripDocument = trips.document(tripId).get().get()
        require(tripDocument.exists()) { "Chuyến đi không tồn tại" }

        val payerDocument = participants.document(paidBy).get().get()
        require(payerDocument.exists()) { "Người thanh toán không tồn tại" }
        require(payerDocument.getString("tripId") == tripId) {
            "Người thanh toán không thuộc chuyến đi này"
        }

        participantIds.distinct().forEach { participantId ->
            val participantDocument = participants.document(participantId).get().get()
            require(participantDocument.exists()) {
                "Người tham gia chi phí không tồn tại: $participantId"
            }
            require(participantDocument.getString("tripId") == tripId) {
                "Người tham gia chi phí không thuộc chuyến đi này"
            }
        }

        val document = expenses.document()
        val data = mapOf(
            "tripId" to tripId,
            "title" to title,
            "amount" to amount,
            "paidBy" to paidBy,
            "participantIds" to participantIds.distinct(),
            "splitType" to splitType,
            "tripPlaceId" to tripPlaceId,
            "createdAt" to java.util.Date()
        )

        document.set(data).get()

        recalculateTotalSpent(tripId)

        return document.id
    }

    // Lấy danh sách khoản chi của chuyến đi
    fun getExpensesByTrip(tripId: String): List<TripExpense> {
        val result = expenses.whereEqualTo("tripId", tripId).get().get()

        return result.documents.map { document ->
            val rawParticipantIds = document.get("participantIds") as? List<*>
                ?: emptyList<Any>()

            val participantIds = rawParticipantIds.filterIsInstance<String>()

            TripExpense(
                expenseId = document.id,
                tripId = document.getString("tripId") ?: "",
                title = document.getString("title") ?: "",
                amount = (document.get("amount") as? Number)?.toDouble() ?: 0.0,
                paidBy = document.getString("paidBy") ?: "",
                participantIds = participantIds,
                splitType = document.getString("splitType") ?: "equal",
                tripPlaceId = document.getString("tripPlaceId") ?: "",
                createdAt = when (val value = document.get("createdAt")) {
                    is com.google.cloud.Timestamp ->
                        value.toDate().toInstant().toString()
                    is String -> value
                    else -> ""
                }
            )
        }.sortedBy { it.createdAt }
    }

    // Xóa khoản chi
    fun deleteExpense(expenseId: String) {
        val expenseDocument = expenses.document(expenseId).get().get()
        if (!expenseDocument.exists()) return

        val tripId = expenseDocument.getString("tripId") ?: return

        expenses.document(expenseId).delete().get()

        val tripDocument = trips.document(tripId).get().get()
        if (tripDocument.exists()) {
            recalculateTotalSpent(tripId)
        }
    }

    private fun recalculateTotalSpent(tripId: String) {
        val result = expenses
            .whereEqualTo("tripId", tripId)
            .get()
            .get()

        val totalSpent = result.documents.sumOf { document ->
            (document.get("amount") as? Number)?.toDouble() ?: 0.0
        }

        trips.document(tripId)
            .update("totalSpent", totalSpent)
            .get()
    }

    fun syncTotalSpent(tripId: String): Double {
        val tripDocument = trips.document(tripId).get().get()
        require(tripDocument.exists()) { "Chuyến đi không tồn tại" }

        recalculateTotalSpent(tripId)

        val updatedDocument = trips.document(tripId).get().get()
        return (updatedDocument.get("totalSpent") as? Number)?.toDouble() ?: 0.0
    }
}

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
        description: String,
        amount: Long,
        paidByParticipantId: String,
        splitAmounts: Map<String, Long>
    ): String {
        require(description.isNotBlank()) {
            "Nội dung chi tiêu không được để trống"
        }
        require(amount > 0L) {
            "Số tiền phải lớn hơn 0"
        }
        require(splitAmounts.isNotEmpty()) {
            "Phải có ít nhất một người chịu khoản chi"
        }
        require(splitAmounts.values.all { it >= 0L }) {
            "Số tiền chia cho từng người không được âm"
        }
        require(splitAmounts.values.sum() == amount) {
            "Tổng tiền chia phải bằng tổng khoản chi"
        }

        val tripDocument = trips.document(tripId).get().get()
        require(tripDocument.exists()) {
            "Chuyến đi không tồn tại"
        }

        val payerDocument =
            participants.document(paidByParticipantId).get().get()
        require(payerDocument.exists()) {
            "Người thanh toán không tồn tại"
        }
        require(payerDocument.getString("tripId") == tripId) {
            "Người thanh toán không thuộc chuyến đi này"
        }

        splitAmounts.keys.forEach { participantId ->
            val participantDocument =
                participants.document(participantId).get().get()

            require(participantDocument.exists()) {
                "Người chịu chi phí không tồn tại: $participantId"
            }
            require(participantDocument.getString("tripId") == tripId) {
                "Người chịu chi phí không thuộc chuyến đi này"
            }
        }

        val document = expenses.document()

        val data = mapOf(
            "tripId" to tripId,
            "description" to description,
            "amount" to amount,
            "paidByParticipantId" to paidByParticipantId,
            "splitAmounts" to splitAmounts,
            "createdAt" to Instant.now().toString()
        )

        document.set(data).get()

        // Cập nhật tổng tiền đã chi của chuyến đi
        val currentSpent = tripDocument.getLong("totalSpent") ?: 0L
        trips.document(tripId)
            .update("totalSpent", currentSpent + amount)
            .get()

        return document.id
    }

    // Lấy danh sách khoản chi của chuyến đi
    fun getExpensesByTrip(tripId: String): List<TripExpense> {
        val result = expenses
            .whereEqualTo("tripId", tripId)
            .get()
            .get()

        return result.documents.map { document ->
            val rawSplits =
                document.get("splitAmounts") as? Map<*, *> ?: emptyMap<Any, Any>()

            val splitAmounts = rawSplits.mapNotNull { (key, value) ->
                if (key is String && value is Number) {
                    key to value.toLong()
                } else {
                    null
                }
            }.toMap()

            TripExpense(
                expenseId = document.id,
                tripId = document.getString("tripId") ?: "",
                description = document.getString("description") ?: "",
                amount = document.getLong("amount") ?: 0L,
                paidByParticipantId =
                    document.getString("paidByParticipantId") ?: "",
                splitAmounts = splitAmounts,
                createdAt = document.getString("createdAt") ?: ""
            )
        }.sortedBy { it.createdAt }
    }

    // Xóa khoản chi
    fun deleteExpense(expenseId: String) {
        val expenseDocument = expenses.document(expenseId).get().get()

        if (!expenseDocument.exists()) {
            return
        }

        val tripId = expenseDocument.getString("tripId") ?: return
        val amount = expenseDocument.getLong("amount") ?: 0L
        val tripRef = trips.document(tripId)
        val tripDocument = tripRef.get().get()

        expenses.document(expenseId).delete().get()

        if (tripDocument.exists()) {
            val currentSpent = tripDocument.getLong("totalSpent") ?: 0L
            tripRef.update(
                "totalSpent",
                (currentSpent - amount).coerceAtLeast(0L)
            ).get()
        }
    }
}

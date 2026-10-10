package com.example.backend

import com.google.cloud.firestore.DocumentSnapshot

class TripParticipantRepository {

    private val db = FirebaseConfig.getFirestore()

    private val participants = db.collection("trip_participants")
    private val trips = db.collection("trips")

    // Lấy danh sách người tham gia
    fun getParticipantsByTrip(
        tripId: String
    ): List<TripParticipant> {

        val result = participants
            .whereEqualTo("tripId", tripId)
            .get()
            .get()

        return result.documents.map { document ->

            TripParticipant(
                participantId = document.id,
                tripId = document.getString("tripId") ?: "",
                userId = document.getString("userId") ?: "",
                name = document.getString("name") ?: ""
            )
        }
    }

    // Thêm người tham gia chuyến đi
    fun addParticipant(
        tripId: String,
        name: String,
        userId: String = ""
    ): String {

        require(name.isNotBlank()) {
            "Tên người tham gia không được để trống"
        }

        val tripRef = trips.document(tripId)

        val tripDocument = tripRef.get().get()

        require(tripDocument.exists()) {
            "Chuyến đi không tồn tại"
        }

        val participantRef = participants.document()

        val data = mapOf(
            "tripId" to tripId,
            "userId" to userId,
            "name" to name
        )

        db.runTransaction { transaction ->

            val currentTrip: DocumentSnapshot =
                transaction.get(tripRef).get()

            if (!currentTrip.exists()) {
                throw IllegalArgumentException(
                    "Chuyến đi không tồn tại"
                )
            }

            val currentCount =
                currentTrip.getLong("participantCount") ?: 0L

            transaction.set(participantRef, data)

            transaction.update(
                tripRef,
                "participantCount",
                currentCount + 1L
            )

            participantRef.id
        }.get()

        return participantRef.id
    }

    // Xóa người tham gia
    fun removeParticipant(participantId: String) {

        val participantRef = participants.document(participantId)
        val participantDocument = participantRef.get().get()

        if (!participantDocument.exists()) {
            return
        }

        val tripId =
            participantDocument.getString("tripId") ?: return

        val tripRef = trips.document(tripId)

        db.runTransaction { transaction ->

            val participant: DocumentSnapshot =
                transaction.get(participantRef).get()

            if (!participant.exists()) {
                return@runTransaction
            }

            val trip: DocumentSnapshot =
                transaction.get(tripRef).get()

            transaction.delete(participantRef)

            if (trip.exists()) {
                val count =
                    trip.getLong("participantCount") ?: 0L

                transaction.update(
                    tripRef,
                    "participantCount",
                    (count - 1L).coerceAtLeast(0L)
                )
            }
        }.get()
    }
}
package com.example.backend

data class User(
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "user",
    val preferences: List<String> = emptyList(),
    val createdAt: String = ""
)

data class Place(
    val placeId: String = "",
    val name: String = "",
    val category: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val priceLevel: Long = 0,
    val rating: Double = 0.0,
    val reviewCount: Long = 0,
    val googleMapsUri: String = "",
    val websiteUri: String = ""
)

data class FavoritePlace(
    val userId: String = "",
    val placeId: String = "",
    val createdAt: String = ""
)

data class Trip(
    val tripId: String = "",
    val userId: String = "",
    val name: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val budget: Long = 0L,
    val participantCount: Int = 0,
    val createdAt: String = ""
)

data class TripPlace(
    val tripPlaceId: String = "",
    val tripId: String = "",
    val placeId: String = "",
    val order: Int = 0,
    val plannedDate: String = "",
    val note: String = ""
)

data class TripParticipant(
    val participantId: String = "",
    val tripId: String = "",
    val userId: String = "",
    val name: String = ""
)

data class TripExpense(
    val expenseId: String = "",
    val tripId: String = "",
    val description: String = "",
    val amount: Long = 0L,
    val paidByParticipantId: String = "",
    val splitAmounts: Map<String, Long> = emptyMap(),
    val createdAt: String = ""
)

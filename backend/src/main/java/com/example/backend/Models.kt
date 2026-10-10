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
    val budget: Double = 0.0,
    val currency: String = "VND",
    val participantCount: Int = 0,
    val totalSpent: Double = 0.0,
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
    val title: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val participantIds: List<String> = emptyList(),
    val splitType: String = "equal",
    val tripPlaceId: String = "",
    val createdAt: String = ""
)
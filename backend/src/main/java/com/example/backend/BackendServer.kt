package com.example.backend

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.google.gson.Gson
import io.ktor.server.request.*

fun main() {

    // Khởi tạo Firebase
    FirebaseConfig.initialize()

    // Khởi tạo các Repository
    val userRepository = UserRepository()
    val placeRepository = PlaceRepository()
    val favoritePlaceRepository = FavoritePlaceRepository()
    val tripRepository = TripRepository()
    val tripPlaceRepository = TripPlaceRepository()
    val tripParticipantRepository = TripParticipantRepository()
    val tripExpenseRepository = TripExpenseRepository()

    // Khởi động HTTP server
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {

        routing {

            // =========================
            // SERVER
            // =========================

            get("/") {
                call.respondText(
                    """{"message":"Backend is running!"}""",
                    ContentType.Application.Json
                )
            }

            // =========================
            // TRIPS
            // =========================

            // Tạo chuyến đi
            post("/users/{userId}/trips") {
                val userId = call.parameters["userId"]

                if (userId.isNullOrBlank()) {
                    call.respondText(
                        """{"error":"Missing userId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@post
                }

                try {
                    val body = Gson().fromJson(
                        call.receiveText(),
                        Map::class.java
                    )

                    val name = body["name"] as? String ?: ""
                    val startDate = body["startDate"] as? String ?: ""
                    val endDate = body["endDate"] as? String ?: ""
                    val budget = (body["budget"] as? Number)?.toLong() ?: 0L

                    val tripId = tripRepository.createTrip(
                        Trip(
                            userId = userId,
                            name = name,
                            startDate = startDate,
                            endDate = endDate,
                            budget = budget
                        )
                    )

                    call.respondText(
                        buildJson(mapOf("tripId" to tripId)),
                        ContentType.Application.Json,
                        HttpStatusCode.Created
                    )
                } catch (e: Exception) {
                    call.respondText(
                        buildJson(mapOf("error" to (e.message ?: "Invalid request"))),
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                }
            }

            // Lấy danh sách chuyến đi của người dùng
            get("/users/{userId}/trips") {
                val userId = call.parameters["userId"]

                if (userId.isNullOrBlank()) {
                    call.respondText(
                        """{"error":"Missing userId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@get
                }

                val trips = tripRepository.getTripsByUser(userId)

                val json = trips.joinToString(
                    prefix = "[",
                    postfix = "]"
                ) { trip ->
                    buildJson(
                        mapOf(
                            "tripId" to trip.tripId,
                            "userId" to trip.userId,
                            "name" to trip.name,
                            "startDate" to trip.startDate,
                            "endDate" to trip.endDate,
                            "budget" to trip.budget,
                            "participantCount" to trip.participantCount,
                            "createdAt" to trip.createdAt
                        )
                    )
                }

                call.respondText(json, ContentType.Application.Json)
            }

            // Lấy chi tiết một chuyến đi
            get("/trips/{tripId}") {
                val tripId = call.parameters["tripId"]

                if (tripId.isNullOrBlank()) {
                    call.respondText(
                        """{"error":"Missing tripId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@get
                }

                val trip = tripRepository.getTrip(tripId)

                if (trip == null) {
                    call.respondText(
                        """{"error":"Trip not found"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.NotFound
                    )
                    return@get
                }

                call.respondText(
                    buildJson(
                        mapOf(
                            "tripId" to trip.tripId,
                            "userId" to trip.userId,
                            "name" to trip.name,
                            "startDate" to trip.startDate,
                            "endDate" to trip.endDate,
                            "budget" to trip.budget,
                            "participantCount" to trip.participantCount,
                            "createdAt" to trip.createdAt
                        )
                    ),
                    ContentType.Application.Json
                )
            }

            // Lấy danh sách thành viên của chuyến đi
            get("/trips/{tripId}/participants") {
                val tripId = call.parameters["tripId"]

                if (tripId.isNullOrBlank()) {
                    call.respondText(
                        """{"error":"Missing tripId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@get
                }

                try {
                    val participants =
                        tripParticipantRepository.getParticipantsByTrip(tripId)

                    val json = participants.joinToString(
                        prefix = "[",
                        postfix = "]"
                    ) { participant ->
                        buildJson(
                            mapOf(
                                "participantId" to participant.participantId,
                                "tripId" to participant.tripId,
                                "userId" to participant.userId,
                                "name" to participant.name
                            )
                        )
                    }

                    call.respondText(
                        json,
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        buildJson(
                            mapOf("error" to (e.message ?: "Unknown error"))
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Thêm thành viên vào chuyến đi
            post("/trips/{tripId}/participants") {
                val tripId = call.parameters["tripId"]

                if (tripId.isNullOrBlank()) {
                    call.respondText(
                        """{"error":"Missing tripId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@post
                }

                try {
                    val body = Gson().fromJson(
                        call.receiveText(),
                        Map::class.java
                    )

                    val name = body["name"] as? String ?: ""
                    val userId = body["userId"] as? String ?: ""

                    val participantId = tripParticipantRepository.addParticipant(
                        tripId = tripId,
                        name = name,
                        userId = userId
                    )

                    call.respondText(
                        buildJson(
                            mapOf(
                                "participantId" to participantId,
                                "tripId" to tripId,
                                "userId" to userId,
                                "name" to name
                            )
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.Created
                    )
                } catch (e: IllegalArgumentException) {
                    call.respondText(
                        buildJson(
                            mapOf("error" to (e.message ?: "Invalid request"))
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                } catch (e: Exception) {
                    call.respondText(
                        buildJson(
                            mapOf("error" to (e.message ?: "Unknown error"))
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Xóa thành viên khỏi chuyến đi
            delete("/trips/{tripId}/participants/{participantId}") {
                val tripId = call.parameters["tripId"]
                val participantId = call.parameters["participantId"]

                if (tripId.isNullOrBlank() || participantId.isNullOrBlank()) {
                    call.respondText(
                        """{"error":"Missing tripId or participantId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@delete
                }

                try {
                    val participant = tripParticipantRepository
                        .getParticipantsByTrip(tripId)
                        .find { it.participantId == participantId }

                    if (participant == null) {
                        call.respondText(
                            """{"error":"Participant not found in this trip"}""",
                            ContentType.Application.Json,
                            HttpStatusCode.NotFound
                        )
                        return@delete
                    }

                    tripParticipantRepository.removeParticipant(participantId)

                    call.respondText(
                        """{"message":"Participant deleted successfully"}""",
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        buildJson(
                            mapOf("error" to (e.message ?: "Unknown error"))
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Lấy danh sách địa điểm trong chuyến đi
            get("/trips/{tripId}/places") {
                val tripId = call.parameters["tripId"]

                if (tripId.isNullOrBlank()) {
                    call.respondText(
                        """{"error":"Missing tripId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@get
                }

                try {
                    val places = tripPlaceRepository.getPlacesByTrip(tripId)

                    val json = places.joinToString(
                        prefix = "[",
                        postfix = "]"
                    ) { item ->
                        buildJson(
                            mapOf(
                                "tripPlaceId" to item.tripPlaceId,
                                "tripId" to item.tripId,
                                "placeId" to item.placeId,
                                "order" to item.order,
                                "plannedDate" to item.plannedDate,
                                "note" to item.note
                            )
                        )
                    }

                    call.respondText(
                        json,
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        buildJson(
                            mapOf("error" to (e.message ?: "Unknown error"))
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Thêm địa điểm vào chuyến đi
            post("/trips/{tripId}/places") {
                val tripId = call.parameters["tripId"]

                if (tripId.isNullOrBlank()) {
                    call.respondText(
                        """{"error":"Missing tripId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@post
                }

                try {
                    val body = Gson().fromJson(
                        call.receiveText(),
                        Map::class.java
                    )

                    val placeId = body["placeId"] as? String ?: ""
                    val order = (body["order"] as? Number)?.toInt() ?: 0
                    val plannedDate = body["plannedDate"] as? String ?: ""
                    val note = body["note"] as? String ?: ""

                    val tripPlaceId = tripPlaceRepository.addPlace(
                        tripId = tripId,
                        placeId = placeId,
                        order = order,
                        plannedDate = plannedDate,
                        note = note
                    )

                    call.respondText(
                        buildJson(
                            mapOf(
                                "tripPlaceId" to tripPlaceId,
                                "tripId" to tripId,
                                "placeId" to placeId,
                                "order" to order,
                                "plannedDate" to plannedDate,
                                "note" to note
                            )
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.Created
                    )
                } catch (e: IllegalArgumentException) {
                    call.respondText(
                        buildJson(
                            mapOf("error" to (e.message ?: "Invalid request"))
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                } catch (e: Exception) {
                    call.respondText(
                        buildJson(
                            mapOf("error" to (e.message ?: "Unknown error"))
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Xóa địa điểm khỏi lịch trình chuyến đi
            delete("/trips/{tripId}/places/{tripPlaceId}") {
                val tripId = call.parameters["tripId"]
                val tripPlaceId = call.parameters["tripPlaceId"]

                if (tripId.isNullOrBlank() || tripPlaceId.isNullOrBlank()) {
                    call.respondText(
                        """{"error":"Missing tripId or tripPlaceId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@delete
                }

                try {
                    val tripPlaces = tripPlaceRepository.getPlacesByTrip(tripId)
                    val item = tripPlaces.find {
                        it.tripPlaceId == tripPlaceId
                    }

                    if (item == null) {
                        call.respondText(
                            """{"error":"Trip place not found"}""",
                            ContentType.Application.Json,
                            HttpStatusCode.NotFound
                        )
                        return@delete
                    }

                    tripPlaceRepository.removePlace(tripPlaceId)

                    call.respondText(
                        """{"message":"Place removed from trip successfully"}""",
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    call.respondText(
                        buildJson(
                            mapOf("error" to (e.message ?: "Unknown error"))
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // Cập nhật địa điểm trong lịch trình
            put("/trips/{tripId}/places/{tripPlaceId}") {
                val tripId = call.parameters["tripId"]
                val tripPlaceId = call.parameters["tripPlaceId"]

                if (tripId.isNullOrBlank() || tripPlaceId.isNullOrBlank()) {
                    call.respondText(
                        """{"error":"Missing tripId or tripPlaceId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@put
                }

                try {
                    val existingPlaces =
                        tripPlaceRepository.getPlacesByTrip(tripId)

                    val item = existingPlaces.find {
                        it.tripPlaceId == tripPlaceId
                    }

                    if (item == null) {
                        call.respondText(
                            """{"error":"Trip place not found"}""",
                            ContentType.Application.Json,
                            HttpStatusCode.NotFound
                        )
                        return@put
                    }

                    val body = Gson().fromJson(
                        call.receiveText(),
                        Map::class.java
                    )

                    val order = (body["order"] as? Number)?.toInt()
                        ?: item.order
                    val plannedDate = body["plannedDate"] as? String
                        ?: item.plannedDate
                    val note = body["note"] as? String
                        ?: item.note

                    tripPlaceRepository.updateTripPlace(
                        tripPlaceId = tripPlaceId,
                        order = order,
                        plannedDate = plannedDate,
                        note = note
                    )

                    call.respondText(
                        buildJson(
                            mapOf(
                                "tripPlaceId" to tripPlaceId,
                                "tripId" to tripId,
                                "placeId" to item.placeId,
                                "order" to order,
                                "plannedDate" to plannedDate,
                                "note" to note
                            )
                        ),
                        ContentType.Application.Json
                    )
                } catch (e: IllegalArgumentException) {
                    call.respondText(
                        buildJson(
                            mapOf("error" to (e.message ?: "Invalid request"))
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                } catch (e: Exception) {
                    call.respondText(
                        buildJson(
                            mapOf("error" to (e.message ?: "Unknown error"))
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }

            // =========================
            // USERS
            // =========================

            get("/users/{userId}") {

                println(">>> API: nhận request /users/${call.parameters["userId"]}")

                val userId = call.parameters["userId"]

                if (userId == null) {
                    call.respondText(
                        """{"error":"Missing userId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@get
                }

                val user = userRepository.getUser(userId)

                if (user != null) {

                    println(">>> API: đã lấy được User, chuẩn bị tạo JSON")

                    val json = buildJson(
                        mapOf(
                            "username" to user.username,
                            "fullName" to user.fullName,
                            "email" to user.email,
                            "role" to user.role,
                            "preferences" to user.preferences,
                            "createdAt" to user.createdAt
                        )
                    )

                    println(">>> JSON = $json")

                    call.respondText(
                        json,
                        ContentType.Application.Json
                    )
                }
            }

            get("/users") {

                val users = userRepository.getAllUsers()

                val json = users.joinToString(
                    prefix = "[",
                    postfix = "]"
                ) { user ->

                    buildJson(
                        mapOf(
                            "username" to user.username,
                            "fullName" to user.fullName,
                            "email" to user.email,
                            "role" to user.role,
                            "preferences" to user.preferences,
                            "createdAt" to user.createdAt
                        )
                    )
                }

                call.respondText(
                    json,
                    ContentType.Application.Json
                )
            }

            put("/users/{userId}") {

                val userId = call.parameters["userId"]

                if (userId == null) {
                    call.respondText(
                        """{"error":"Missing userId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@put
                }

                userRepository.updateUser(
                    userId = userId,
                    username = "testuser",
                    fullName = "Nguyễn Văn Am",
                    email = "test@gmail.com",
                    role = "user",
                    preferences = listOf(
                        "Ẩm thực",
                        "Du lịch",
                        "Mua sắm"
                    )
                )

                call.respondText(
                    """{"message":"User updated successfully"}""",
                    ContentType.Application.Json
                )
            }

            post("/users/{userId}") {

                val userId = call.parameters["userId"]

                if (userId == null) {
                    call.respondText(
                        """{"error":"Missing userId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@post
                }

                val user = User(
                    username = "testuser2",
                    fullName = "Nguyễn Văn Test",
                    email = "mz@gmail.com",
                    role = "user",
                    preferences = listOf(
                        "Ẩm thực",
                        "Du lịch"
                    ),
                    createdAt = System.currentTimeMillis().toString()
                )

                userRepository.createUser(
                    userId = userId,
                    user = user
                )

                call.respondText(
                    """{"message":"User created successfully"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.Created
                )
            }

            delete("/users/{userId}") {

                val userId = call.parameters["userId"]

                if (userId == null) {
                    call.respondText(
                        """{"error":"Missing userId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@delete
                }

                userRepository.deleteUser(userId)

                call.respondText(
                    """{"message":"User deleted successfully"}""",
                    ContentType.Application.Json
                )
            }

            // =========================
            // PLACES
            // =========================

            get("/places/{placeId}") {

                val placeId = call.parameters["placeId"]

                if (placeId == null) {
                    call.respondText(
                        """{"error":"Missing placeId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@get
                }

                val place = placeRepository.getPlace(placeId)

                if (place != null) {
                    call.respondText(
                        buildJson(
                            mapOf(
                                "placeId" to place.placeId,
                                "name" to place.name,
                                "category" to place.category,
                                "address" to place.address,
                                "latitude" to place.latitude,
                                "longitude" to place.longitude,
                                "priceLevel" to place.priceLevel,
                                "rating" to place.rating,
                                "reviewCount" to place.reviewCount,
                                "googleMapsUri" to place.googleMapsUri,
                                "websiteUri" to place.websiteUri
                            )
                        ),
                        ContentType.Application.Json
                    )
                } else {
                    call.respondText(
                        """{"error":"Place not found"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.NotFound
                    )
                }
            }

            // =========================
            // ALL PLACES
            // =========================

            get("/places") {

                val places = placeRepository.getAllPlaces()

                val json = places.joinToString(
                    prefix = "[",
                    postfix = "]"
                ) { place ->

                    buildJson(
                        mapOf(
                            "placeId" to place.placeId,
                            "name" to place.name,
                            "category" to place.category,
                            "address" to place.address,
                            "latitude" to place.latitude,
                            "longitude" to place.longitude,
                            "priceLevel" to place.priceLevel,
                            "rating" to place.rating,
                            "reviewCount" to place.reviewCount,
                            "googleMapsUri" to place.googleMapsUri,
                            "websiteUri" to place.websiteUri
                        )
                    )
                }

                call.respondText(
                    json,
                    ContentType.Application.Json
                )
            }

            // =========================
            // FAVORITES
            // =========================

            get("/users/{userId}/favorites") {

                val userId = call.parameters["userId"]

                if (userId == null) {
                    call.respondText(
                        """{"error":"Missing userId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@get
                }

                val favorites =
                    favoritePlaceRepository.getFavoritesByUser(userId)

                val json = favorites.joinToString(
                    prefix = "[",
                    postfix = "]"
                ) { favorite ->

                    buildJson(
                        mapOf(
                            "userId" to favorite.userId,
                            "placeId" to favorite.placeId,
                            "createdAt" to favorite.createdAt
                        )
                    )
                }

                call.respondText(
                    json,
                    ContentType.Application.Json
                )
            }

            get("/users/{userId}/favorites/{placeId}") {

                val userId = call.parameters["userId"]
                val placeId = call.parameters["placeId"]

                if (userId == null || placeId == null) {
                    call.respondText(
                        """{"error":"Missing userId or placeId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@get
                }

                val isFavorite = favoritePlaceRepository.isFavorite(
                    userId,
                    placeId
                )

                call.respondText(
                    """{"isFavorite":$isFavorite}""",
                    ContentType.Application.Json
                )
            }

            post("/users/{userId}/favorites/{placeId}") {

                val userId = call.parameters["userId"]
                val placeId = call.parameters["placeId"]

                if (userId == null || placeId == null) {
                    call.respondText(
                        """{"error":"Missing userId or placeId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@post
                }

                favoritePlaceRepository.addFavorite(
                    userId,
                    placeId
                )

                call.respondText(
                    """{"message":"Favorite added successfully"}""",
                    ContentType.Application.Json,
                    HttpStatusCode.Created
                )
            }

            delete("/users/{userId}/favorites/{placeId}") {

                val userId = call.parameters["userId"]
                val placeId = call.parameters["placeId"]

                if (userId == null || placeId == null) {
                    call.respondText(
                        """{"error":"Missing userId or placeId"}""",
                        ContentType.Application.Json,
                        HttpStatusCode.BadRequest
                    )
                    return@delete
                }

                favoritePlaceRepository.removeFavorite(
                    userId,
                    placeId
                )

                call.respondText(
                    """{"message":"Favorite removed successfully"}""",
                    ContentType.Application.Json
                )
            }
        }
    }.start(wait = true)
}

/**
 * Chuyển dữ liệu thành JSON.
 */
fun buildJson(data: Map<String, Any?>): String {

    return data.entries.joinToString(
        prefix = "{",
        postfix = "}"
    ) { (key, value) ->

        "\"${escapeJson(key)}\":${valueToJson(value)}"
    }
}

/**
 * Chuyển từng kiểu dữ liệu sang JSON.
 */
fun valueToJson(value: Any?): String {

    return when (value) {

        null -> "null"

        is String ->
            "\"${escapeJson(value)}\""

        is Number, is Boolean ->
            value.toString()

        is List<*> ->
            value.joinToString(
                prefix = "[",
                postfix = "]"
            ) {
                valueToJson(it)
            }

        else ->
            "\"${escapeJson(value.toString())}\""
    }
}

/**
 * Escape ký tự đặc biệt trong JSON.
 */
fun escapeJson(value: String): String {

    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
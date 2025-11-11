package com.aliumar.fypfinal1

data class User(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Double = 0.0,
    var avgRating: Double = 0.0,
    var ratingCount: Int = 0
)

data class Repairman(
    var id: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Double = 0.0,
    val storeName: String = "",
    // Initialized as empty list to track specialties
    val specialties: List<String> = listOf(),
    // NEW: Service description field
    var serviceDescription: String = "",
    var avgRating: Double = 0.0,
    var ratingCount: Int = 0,
    // NEW: Flag to force setup completion after registration
    var isSetupComplete: Boolean = false,
    // NEW: Admin approval flag
    var isApprovedByAdmin: Boolean = false, // <--- MANDATORY FIELD FOR ADMIN APPROVAL
    // The adapter now calculates the city dynamically
    val city: String = ""
)
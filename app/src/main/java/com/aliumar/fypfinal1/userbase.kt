package com.aliumar.fypfinal1

data class User(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Double = 0.0
)

data class Repairman(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val rating: Double = 0.0,
    val storeName: String = "",
    val specialties: List<String> = listOf("")
)

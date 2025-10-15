package com.aliumar.fypfinal1

data class User(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val balance: Double,
    val rating: Double
)

data class Repairman(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val rating: Double
)

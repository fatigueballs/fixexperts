package com.aliumar.fypfinal1

data class ServiceRequest(
    var id: String = "",
    var userId: String = "",
    var userName: String = "",
    var repairmanId: String = "",
    var repairmanName: String = "",
    var serviceType: String = "",
    var date: String = "",
    var dateMillis: Long = 0L,
    var status: String = "Pending",
    var timestamp: Long = 0L,
    var userRated: Boolean = false,
    var repairmanRated: Boolean = false,
    var userConfirmedJobDone: Boolean = false,
    var repairmanConfirmedPayment: Boolean = false,
    // NEW FIELD
    var problemDescription: String = ""
)
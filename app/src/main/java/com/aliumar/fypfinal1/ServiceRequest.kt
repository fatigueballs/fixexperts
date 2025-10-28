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
    // CRITICAL FIX: Use "Pending" to match RepairmanRequestsActivity and RequestAdapter
    var status: String = "Pending",
    var timestamp: Long = 0L,
    var userRated: Boolean = false,
    var repairmanRated: Boolean = false,
    // NEW: Fields for multi-step completion and payment, roles SWAPPED.
    // This is now set by the USER to confirm the job is physically done
    var userConfirmedJobDone: Boolean = false,
    // This is now set by the REPAIRMAN to confirm the payment is received
    var repairmanConfirmedPayment: Boolean = false
)
package com.aliumar.fypfinal1

data class ChatMessage(
    var messageId: String = "",
    var senderId: String = "",
    var message: String = "",
    var timestamp: Long = 0L
)
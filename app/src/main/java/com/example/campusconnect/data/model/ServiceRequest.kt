package com.example.campusconnect.data.model

data class ServiceRequest(
    val id: String = "",
    val requesterName: String = "",
    val studentId: String = "",
    val service: String = "",
    val description: String = "",
    val status: String = "Pending",
    val date: String = "",
    val documentUrl: String? = null,
    val isRead: Boolean = false
)

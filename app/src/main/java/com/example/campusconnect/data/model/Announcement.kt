package com.example.campusconnect.data.model

import java.util.UUID

data class Announcement(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val date: String = "",
    val location: String = "",
    val description: String = "",
    val participants: List<String> = emptyList() // List of student names or IDs who joined
)

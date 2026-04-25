package com.example.campusconnect.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "Student"
) {
    companion object {
        const val ROLE_ADMIN = "Admin"
        const val ROLE_STUDENT = "Student"
    }
}

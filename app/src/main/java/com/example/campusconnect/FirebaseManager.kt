package com.example.campusconnect

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    suspend fun createUserInFirestore(uid: String, name: String, email: String, role: String) {
        val user = User(uid = uid, name = name, email = email, role = role)
        db.collection("users").document(uid).set(user).await()
    }

    suspend fun getUserRole(uid: String): String? {
        val document = db.collection("users").document(uid).get().await()
        return document.getString("role")
    }

    suspend fun getUserData(uid: String): User? {
        val document = db.collection("users").document(uid).get().await()
        return if (document.exists()) {
            document.toObject(User::class.java)
        } else {
            null
        }
    }

    suspend fun getAllUsers(): List<User> {
        val snapshot = db.collection("users").get().await()
        return snapshot.toObjects(User::class.java)
    }
}

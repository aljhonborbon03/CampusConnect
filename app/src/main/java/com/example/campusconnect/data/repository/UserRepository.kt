package com.example.campusconnect.data.repository

import com.example.campusconnect.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun createUser(uid: String, name: String, email: String, role: String) {
        val user = User(uid = uid, name = name, email = email, role = role)
        db.collection("users").document(uid).set(user).await()
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
    
    fun getCurrentUserUid(): String? = auth.currentUser?.uid
    
    fun signOut() = auth.signOut()
}

package com.example.campusconnect.data.repository

import android.content.Context
import android.util.Log
import com.example.campusconnect.data.model.Announcement
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AnnouncementRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val collection = db.collection("announcements")

    val announcements: Flow<List<Announcement>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error fetching announcements: ${error.message}")
                // Send empty list instead of closing with error to prevent crash
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot?.toObjects(Announcement::class.java) ?: emptyList()
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveAnnouncements(announcements: List<Announcement>) {
        announcements.forEach { announcement ->
            try {
                collection.document(announcement.id).set(announcement).await()
            } catch (e: Exception) {
                Log.e("Firestore", "Error saving announcement: ${e.message}")
            }
        }
    }

    suspend fun addAnnouncement(announcement: Announcement) {
        collection.document(announcement.id).set(announcement).await()
    }

    suspend fun deleteAnnouncement(announcementId: String) {
        collection.document(announcementId).delete().await()
    }

    suspend fun joinEvent(announcementId: String, userName: String) {
        collection.document(announcementId).update(
            "participants", FieldValue.arrayUnion(userName)
        ).await()
    }
}

package com.example.campusconnect.data.repository

import android.content.Context
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
                close(error)
                return@addSnapshotListener
            }
            val items = snapshot?.toObjects(Announcement::class.java) ?: emptyList()
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    suspend fun saveAnnouncements(announcements: List<Announcement>) {
        // This method was used for local storage, for Firestore we usually add one by one
        // But for compatibility with existing code during migration:
        announcements.forEach { announcement ->
            collection.document(announcement.id).set(announcement).await()
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

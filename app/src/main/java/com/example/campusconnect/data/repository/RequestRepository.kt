package com.example.campusconnect.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.campusconnect.data.model.ServiceRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RequestRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val collection = db.collection("requests")

    val requests: Flow<List<ServiceRequest>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error fetching requests: ${error.message}")
                // Send an empty list instead of closing with error to prevent app crash
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot?.toObjects(ServiceRequest::class.java) ?: emptyList()
            trySend(items)
        }
        awaitClose { listener.remove() }
    }

    suspend fun addRequest(request: ServiceRequest) {
        collection.document(request.id).set(request).await()
    }

    suspend fun updateRequestStatus(requestId: String, newStatus: String) {
        val updates = hashMapOf(
            "status" to newStatus,
            "isRead" to false
        )
        collection.document(requestId).update(updates as Map<String, Any>).await()
    }

    suspend fun markAsRead(requestId: String) {
        collection.document(requestId).update("isRead", true).await()
    }

    suspend fun deleteRequest(requestId: String) {
        collection.document(requestId).delete().await()
    }

    suspend fun uploadDocument(requestId: String, fileUri: Uri): String {
        val cleanId = requestId.trim()
        val mimeType = context.contentResolver.getType(fileUri) ?: "application/octet-stream"
        val extension = when {
            mimeType == "application/pdf" -> "pdf"
            mimeType.startsWith("image/") -> "jpg"
            else -> "file"
        }
        
        val fileName = "documents/${cleanId}_${System.currentTimeMillis()}.$extension"
        val storageRef = storage.reference.child(fileName)
        
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()

        return try {
            storageRef.putFile(fileUri, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            val updates = hashMapOf(
                "documentUrl" to downloadUrl,
                "status" to "Completed",
                "isRead" to false
            )
            collection.document(cleanId).update(updates as Map<String, Any>).await()
            downloadUrl
        } catch (e: Exception) {
            Log.e("Storage", "Upload failed: ${e.message}")
            throw Exception(e.localizedMessage ?: "Unknown Upload Error")
        }
    }
}

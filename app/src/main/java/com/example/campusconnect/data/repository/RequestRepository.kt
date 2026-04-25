package com.example.campusconnect.data.repository

import android.content.Context
import android.net.Uri
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
    
    // Using default instance allows Firebase to automatically find the bucket from your google-services.json
    private val storage = FirebaseStorage.getInstance()
    private val collection = db.collection("requests")

    val requests: Flow<List<ServiceRequest>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
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
        
        // 1. Get correct MIME type
        val mimeType = context.contentResolver.getType(fileUri) ?: "application/octet-stream"
        val extension = when {
            mimeType == "application/pdf" -> "pdf"
            mimeType.startsWith("image/") -> "jpg"
            else -> "file"
        }
        
        // 2. Create the file path
        val fileName = "documents/${cleanId}_${System.currentTimeMillis()}.$extension"
        val storageRef = storage.reference.child(fileName)
        
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()

        return try {
            // 3. Upload file
            storageRef.putFile(fileUri, metadata).await()
            
            // 4. Get the URL
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            // 5. Update Firestore
            val updates = hashMapOf(
                "documentUrl" to downloadUrl,
                "status" to "Completed",
                "isRead" to false
            )
            collection.document(cleanId).update(updates as Map<String, Any>).await()
            
            downloadUrl
        } catch (e: Exception) {
            e.printStackTrace()
            // Provide a more descriptive error if it still fails
            throw Exception(e.localizedMessage ?: "Unknown Upload Error")
        }
    }
}

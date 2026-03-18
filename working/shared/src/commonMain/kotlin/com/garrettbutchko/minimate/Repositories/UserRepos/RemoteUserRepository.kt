package com.garrettbutchko.minimate.repositories

import com.garrettbutchko.minimate.datamodels.User
import com.garrettbutchko.minimate.datamodels.UserDTO
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.storage.storage
import dev.gitlive.firebase.storage.Data

class RemoteUserRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val storage = Firebase.storage
    private val usersCollection = db.collection("users")

    suspend fun fetch(id: String): User? {
        return try {
            val snapshot = usersCollection.document(id).get()
            if (snapshot.exists) {
                snapshot.data<UserDTO>().toUser()
            } else {
                null
            }
        } catch (e: Exception) {
            println("❌ Firestore fetch error: ${e.message}")
            null
        }
    }

    suspend fun save(user: User, updateLastUpdated: Boolean = true): Result<Boolean> {
        return try {
            val updatedUser = if (updateLastUpdated) {
                user.copy(lastUpdated = Timestamp.now())
            } else {
                user
            }
            usersCollection.document(user.googleId).set(updatedUser.toDTO(), merge = true)
            Result.success(true)
        } catch (e: Exception) {
            println("❌ Firestore save error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun delete(id: String): Result<Boolean> {
        return try {
            val ref = usersCollection.document(id)
            val snapshot = ref.get()
            if (!snapshot.exists) {
                println("⚠️ User doc does not exist")
                return Result.success(true)
            }
            ref.delete()
            Result.success(true)
        } catch (e: Exception) {
            println("❌ Firestore delete error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun uploadProfilePhoto(
        id: String,
        imageData: Data
    ): Result<String> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("No signed-in user"))

        return try {
            val ref = storage.reference
                .child("profile_pictures")
                .child("$id.jpg")

            ref.putData(imageData)
            val url = ref.getDownloadUrl()
            
            try {
                // In GitLive Firebase SDK, updateProfile uses photoUrl (String)
                currentUser.updateProfile(photoUrl = url)
            } catch (e: Exception) {
                println("⚠️ Failed to set Auth photoURL: ${e.message}")
            }

            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

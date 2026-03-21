package com.garrettbutchko.minimate.repositories.userRepos

import co.touchlab.kermit.Logger
import com.garrettbutchko.minimate.room.dao.UserDao
import com.garrettbutchko.minimate.datamodels.UserModel
import dev.gitlive.firebase.firestore.Timestamp

class LocalUserRepository(private val userDao: UserDao) {



    private val log = Logger.withTag("UserLocalRepo")

    /**
     * Replaces Swift fetch(id: String).
     * Returns the User model or null if not found.
     */
    suspend fun fetch(id: String): UserModel? {
        return try {
            userDao.fetch(id)
        } catch (e: Exception) {
            log.e(e) { "❌ Local fetch error for id: $id" }
            null
        }
    }

    /**
     * Replaces Swift save(model:updatedLastUpdated:completion:).
     * Handles the timestamp logic and the database insert.
     */
    suspend fun save(userModel: UserModel, updatedLastUpdated: Boolean = true): Result<Boolean> {
        return try {
            // In Kotlin, data classes are immutable. We use .copy() to update the date.
            val userToSave = if (updatedLastUpdated) {
                userModel.copy(lastUpdated = Timestamp.now())
            } else {
                userModel
            }

            userDao.save(userToSave)
            log.d { "📦 Local save successful for: ${userToSave.googleId}" }
            Result.success(true)
        } catch (e: Exception) {
            log.e(e) { "❌ Local save error" }
            Result.success(false)
        }
    }

    /**
     * Replaces Swift delete(id:completion:).
     * In SQL, deleting a non-existent ID doesn't throw an error, so we simply execute.
     */
    suspend fun delete(id: String): Result<Boolean> {
        return try {
            userDao.delete(id)
            log.d { "🗑️ Local delete successful for id: $id" }
            Result.success(true)
        } catch (e: Exception) {
            log.e(e) { "❌ Local delete error for id: $id" }
            Result.success(false)
        }
    }
}
package com.garrettbutchko.minimate.repositories.userRepos

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import co.touchlab.kermit.Logger
import com.garrettbutchko.minimate.dataModels.gameModels.Game
import com.garrettbutchko.minimate.datamodels.UserModel
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.*
import kotlin.math.abs
import com.garrettbutchko.minimate.enums.SignInMethod
import com.garrettbutchko.minimate.viewModels.FirebaseUserSmallData
import dev.gitlive.firebase.storage.Data

class UserRepository(
    val localRepo: LocalUserRepository,
    val remoteRepo: RemoteUserRepository
) {
    private val log = Logger.withTag("UserRepository")
    private val repositoryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Replaces loadOrCreateUserAsync.
     * Uses a "Fast-First" approach: returns local immediately, then reconciles in background.
     */
    @DefaultArgumentInterop.Enabled
    suspend fun loadOrCreateUser(
        id: String,
        firebaseUser: FirebaseUserSmallData? = null,
        name: String? = null,
        signInMethod: SignInMethod? = null,
        appleId: String? = null,
        guestGame: Game? = null,
        onUpdate: (UserModel) -> Unit
    ): UserModel {
        // 1️⃣ Local Phase: Check database first
        val local = localRepo.fetch(id)
        if (local != null) {
            log.d { "✅ Found local user immediately" }
            onUpdate(local)

            // 2️⃣ Background Reconcile: Don't block the UI
            repositoryScope.launch {
                val remote = remoteRepo.fetch(id)
                reconcile(local, remote, id, firebaseUser, name, signInMethod, appleId, guestGame, onUpdate)
            }
            return local
        }

        // 3️⃣ No Local: Fetch remote and reconcile synchronously
        val remote = remoteRepo.fetch(id)
        return reconcile(null, remote, id, firebaseUser, name, signInMethod, appleId, guestGame, onUpdate)
    }

    private suspend fun reconcile(
        local: UserModel?,
        remote: UserModel?,
        id: String,
        firebaseUser: FirebaseUserSmallData?,
        name: String?,
        signInMethod: SignInMethod? = null,
        appleId: String?,
        guestGame: Game?,
        onUpdate: (UserModel) -> Unit
    ): UserModel {
        val result = when {
            local != null && remote != null -> {
                val delta = abs(local.lastUpdated.seconds - remote.lastUpdated.seconds)

                if (delta < 1) { // Already in sync
                    log.d { "🔄 Already in sync" }
                    val updatedLocal = addAccountTypeIfNeeded(local, signInMethod)
                    if (updatedLocal != local) remoteRepo.save(updatedLocal, false)
                    updatedLocal
                } else if (local.lastUpdated.seconds > remote.lastUpdated.seconds) {
                    log.d { "🔄 Local → Remote" }
                    val updatedLocal = addAccountTypeIfNeeded(local, signInMethod)
                    remoteRepo.save(updatedLocal, false)
                    updatedLocal
                } else {
                    log.d { "🔄 Remote → Local" }
                    val updatedRemote = addAccountTypeIfNeeded(remote, signInMethod)
                    localRepo.save(updatedRemote, false)
                    updatedRemote
                }
            }
            local != null && remote == null -> {
                log.d { "🔄 Local → Remote (no remote)" }
                val updatedLocal = addAccountTypeIfNeeded(local, signInMethod)
                remoteRepo.save(updatedLocal, false)
                updatedLocal
            }
            local == null && remote != null -> {
                log.d { "🔄 Remote → Local (no local)" }
                val updatedRemote = addAccountTypeIfNeeded(remote, signInMethod)
                localRepo.save(updatedRemote, false)
                updatedRemote
            }
            else -> {
                log.d { "🆕 Creating new user" }
                createUser(id, firebaseUser, name, signInMethod, appleId, guestGame)
            }
        }
        onUpdate(result)
        return result
    }

    private suspend fun createUser(
        id: String,
        firebaseUser: FirebaseUserSmallData?,
        name: String?,
        signInMethod: SignInMethod?,
        appleId: String?,
        guestGame: Game?
    ): UserModel {
        val finalName = name ?: firebaseUser?.displayName ?: "User#${id.take(5)}"
        val finalEmail = firebaseUser?.email ?: "Email"

        val gameIDs = if (guestGame != null) listOf(guestGame.id) else emptyList()
        val accountTypes = if (signInMethod != null) listOf(signInMethod.value) else emptyList()

        val newUserModel = UserModel(
            googleId = id,
            appleId = appleId,
            name = finalName,
            photoURL = firebaseUser?.photoURL,
            email = finalEmail,
            gameIDs = gameIDs,
            accountType = accountTypes,
            lastUpdated = Timestamp.now()
        )

        // Save to both sources
        localRepo.save(newUserModel, false)
        remoteRepo.save(newUserModel, false)
        return newUserModel
    }

    private fun addAccountTypeIfNeeded(userModel: UserModel, type: SignInMethod?): UserModel {
        if (type == null || userModel.accountType.contains(type.value)) return userModel
        return userModel.copy(accountType = userModel.accountType + type.value)
    }

    suspend fun saveUnified(id: String, userModel: UserModel): Pair<Boolean, Boolean> {
        val localSuccess = localRepo.save(userModel)
        val remoteSuccess = remoteRepo.save(userModel)
        return Pair(localSuccess.isSuccess, remoteSuccess.isSuccess)
    }

    suspend fun deleteUnified(id: String) {
        localRepo.delete(id)
        remoteRepo.delete(id)
    }

    suspend fun uploadProfilePhoto(
        id: String,
        fileData: Data
    ): Result<String> {
        val log = Logger.withTag("UserRepository")

        return try {
            // 1. Upload the raw 'Data' object to Firebase Storage via RemoteRepo
            // This returns the download URL String
            val newPhotoUrl = remoteRepo.uploadProfilePhoto(id, fileData).getOrNull()

            if (newPhotoUrl != null) {
                // 2. Fetch the current local user to prepare the update
                val currentLocalUser = localRepo.fetch(id)

                if (currentLocalUser != null) {
                    // 3. Create a brand new copy of the user with the new URL and current time
                    val updatedUser = currentLocalUser.copy(
                        photoURL = newPhotoUrl,
                        lastUpdated = Timestamp.now()
                    )

                    // 4. Save this updated user to BOTH Local (Room) and Remote (Firestore)
                    // This replaces your DispatchQueue.main.async { self.saveUnified(...) } logic
                    val (localOK, remoteOK) = saveUnified(id, updatedUser)

                    if (localOK && remoteOK) {
                        log.d { "✅ Profile photo URL synced across all data sources" }
                    } else {
                        log.w { "⚠️ Photo uploaded, but database sync partially failed. Local: $localOK, Remote: $remoteOK" }
                    }
                } else {
                    log.w { "⚠️ No local user found for id: $id. Skipping database update." }
                }
                // 5. Return the successful URL (Equivalent to completion(.success(url)))
                Result.success(newPhotoUrl)
            } else {
                log.e { "❌ Failed to get new photo URL after upload" }
                return Result.failure(Exception("Failed to upload photo"))
            }

        } catch (e: Exception) {
            log.e(e) { "❌ Failed to upload profile photo for user: $id" }
            Result.failure(e)
        }
    }
}
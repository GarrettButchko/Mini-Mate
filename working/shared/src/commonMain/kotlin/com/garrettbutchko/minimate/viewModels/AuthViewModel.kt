package com.garrettbutchko.minimate.viewModels

import co.touchlab.kermit.Logger
import com.garrettbutchko.minimate.datamodels.Game
import com.garrettbutchko.minimate.datamodels.UserModel
import com.garrettbutchko.minimate.repositories.FirebaseAuthRepository
import com.garrettbutchko.minimate.repositories.userRepos.UserRepository
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.garrettbutchko.minimate.interfaces.AppNavigationManaging


open class AuthViewModel(
    val authRepository: FirebaseAuthRepository = FirebaseAuthRepository(),
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val log = Logger.withTag("AuthViewModel")

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(authRepository.currentUser)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val _userModelModel = MutableStateFlow<UserModel?>(null)
    val userModel: StateFlow<UserModel?> = _userModelModel.asStateFlow()

    private val _isLoadingUser = MutableStateFlow(false)
    val isLoadingUser: StateFlow<Boolean> = _isLoadingUser.asStateFlow()

    var currentNonce: String? = null

    val currentUserIdentifier: String?
        get() = _firebaseUser.value?.uid

    fun setUserModel(userModel: UserModel?) {
        _userModelModel.value = userModel
    }

    fun setLoading(state: Boolean) {
        _isLoadingUser.value = state
    }

    fun updateUserName(name: String) {
        val currentUser = _userModelModel.value
        if (currentUser != null) {
            _userModelModel.value = currentUser.copy(name = name)
        }
    }

    fun refreshUID() {
        _firebaseUser.value = authRepository.currentUser
    }

    suspend fun refreshVerificationStatus(): Boolean {
        return authRepository.refreshVerificationStatus()
    }

    suspend fun createUser(email: String, password: String): Result<FirebaseUser?> {
        val result = authRepository.createUser(email, password)
        if (result.isSuccess) {
            _firebaseUser.value = result.getOrNull()
        }
        return result
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser?> {
        val result = authRepository.signIn(email, password)
        if (result.isSuccess) {
            _firebaseUser.value = result.getOrNull()
        }
        return result
    }

    open fun logout() {
        coroutineScope.launch {
            authRepository.logout()
            _firebaseUser.value = null
        }
    }

    suspend fun deleteAccount(credential: AuthCredential? = null): Result<Unit> {
        return if (credential != null) {
            val result = authRepository.deleteAccount(credential)
            if (result.isSuccess) {
                _firebaseUser.value = null
            }
            result
        } else {
            Result.failure(Exception("Missing credentials for deletion"))
        }
    }

    suspend fun reauthenticateWithEmail(email: String, password: String): Result<AuthCredential> {
        return try {
            // Firebase uses EmailAuthProvider.credential
            val credential = dev.gitlive.firebase.auth.EmailAuthProvider.credential(email, password)
            val result = authRepository.reauthenticate(credential)
            if (result.isSuccess) {
                Result.success(credential)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown reauthentication error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun reauthenticateWithGoogle(): Result<AuthCredential> {
        return Result.failure(Exception("Not implemented on this platform"))
    }

    fun signInWithGoogleTokens(
        idToken: String,
        accessToken: String?,
        completion: (Result<FirebaseUser?>) -> Unit
    ) {
        val credential = GoogleAuthProvider.credential(idToken, accessToken)

        coroutineScope.launch {
            val result = authRepository.signInWithCredential(credential)
            if (result.isSuccess) {
                val user = result.getOrNull()
                _firebaseUser.value = user
                completion(Result.success(user))
            } else {
                completion(Result.failure(result.exceptionOrNull() ?: Exception("Unknown error during Google sign in")))
            }
        }
    }

    fun getGoogleCredential(
        idToken: String,
        accessToken: String?,
        completion: (Result<AuthCredential>) -> Unit
    ) {
        try {
            val credential = GoogleAuthProvider.credential(idToken, accessToken)
            completion(Result.success(credential))
        } catch (e: Exception) {
            completion(Result.failure(e))
        }
    }

    fun createOrSignInUserAndNavigateToHome(
        userRepo: UserRepository,
        viewManager: AppNavigationManaging,
        user: FirebaseUser,
        name: String? = null,
        signInMethod: com.garrettbutchko.minimate.enums.SignInMethod? = null,
        appleId: String? = null,
        navToHome: Boolean = true,
        guestGame: Game? = null,
        onErrorMessage: (String?, Boolean) -> Unit,
        onClearGuestGame: () -> Unit,
        completion: () -> Unit = {}
    ) {
        onErrorMessage(null, false)
        coroutineScope.launch {
            try {
                // Wait for UserRepository to resolve the user
                val loadedUser = userRepo.loadOrCreateUser(
                    id = user.uid,
                    firebaseUser = user,
                    name = name,
                    signInMethod = signInMethod,
                    appleId = appleId,
                    guestGame = guestGame
                ) { immediateLocalUser ->
                    setUserModel(immediateLocalUser)
                }
                
                // Final fully-resolved remote/local reconciled user
                setUserModel(loadedUser)
                
                // If the guest game was used as part of creation, we clear it out.
                // UserRepository doesn't expose a "creation" boolean directly in loadOrCreateUser anymore, 
                // but we can just invoke onClearGuestGame if a guestGame was provided.
                if (guestGame != null) {
                    onClearGuestGame()
                }

                if (navToHome) {
                    viewManager.navigateAfterSignIn()
                }
            } catch (e: Exception) {
                log.e(e) { "Failed to load or create user" }
                onErrorMessage(e.message, false)
            } finally {
                completion()
            }
        }
    }

    fun signInUIManage(
        emailInput: String,
        passwordInput: String,
        userRepo: UserRepository,
        viewManager: AppNavigationManaging,
        guestGame: Game? = null,
        onClearForm: () -> Unit,
        onShowSignUp: () -> Unit,
        onErrorMessage: (String?, Boolean) -> Unit,
        onClearGuestGame: () -> Unit,
        completion: () -> Unit = {}
    ) {
        coroutineScope.launch {
            val result = signIn(emailInput, passwordInput)
            if (result.isFailure) {
                onShowSignUp()
                onErrorMessage("No User Found Please Sign Up", false)
                completion()
                return@launch
            }
            
            val firebaseUser = result.getOrNull()
            if (firebaseUser != null) {
                if (firebaseUser.isEmailVerified) {
                    createOrSignInUserAndNavigateToHome(
                        userRepo = userRepo,
                        viewManager = viewManager,
                        user = firebaseUser,
                        signInMethod = com.garrettbutchko.minimate.enums.SignInMethod.EMAIL,
                        guestGame = guestGame,
                        onErrorMessage = onErrorMessage,
                        onClearGuestGame = onClearGuestGame,
                        completion = completion
                    )
                } else {
                    val verificationResult = authRepository.sendEmailVerification()
                    if (verificationResult.isFailure) {
                        val error = verificationResult.exceptionOrNull()?.message ?: "Unknown error"
                        onErrorMessage("Couldn’t send verification email: ${error}", false)
                    } else {
                        onClearForm()
                        onErrorMessage("Please Verify Your Email To Continue", true)
                        logout() // Logout AFTER email is sent
                    }
                    completion()
                }
            } else {
                completion()
            }
        }
    }
}

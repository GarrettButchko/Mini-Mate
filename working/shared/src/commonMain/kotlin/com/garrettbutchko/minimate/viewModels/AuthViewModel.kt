package com.garrettbutchko.minimate.viewModels

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import co.touchlab.kermit.Logger
import com.garrettbutchko.minimate.dataModels.gameModels.Game
import com.garrettbutchko.minimate.datamodels.UserModel
import com.garrettbutchko.minimate.enums.SignInMethod
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
import dev.gitlive.firebase.auth.EmailAuthProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class FirebaseUserSmallData (
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoURL: String?
)

open class AuthViewModel(
    val authRepository: FirebaseAuthRepository = FirebaseAuthRepository(),
    val viewManager: AppNavigationManaging,
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : KoinComponent {
    
    // Breaking circular dependency with lazy injection
    val userRepository: UserRepository by inject()

    private val log = Logger.withTag("AuthViewModel")

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(authRepository.currentUser)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val _userModel = MutableStateFlow<UserModel?>(null)
    val userModel: StateFlow<UserModel?> = _userModel.asStateFlow()

    private val _isLoadingUser = MutableStateFlow(false)
    val isLoadingUser: StateFlow<Boolean> = _isLoadingUser.asStateFlow()

    var currentNonce: String? = null

    val currentUserIdentifier: String?
        get() = _firebaseUser.value?.uid

    fun setUserModel(userModel: UserModel?) {
        _userModel.value = userModel
    }

    fun setLoading(state: Boolean) {
        _isLoadingUser.value = state
    }

    fun updateUserName(name: String) {
        val currentUser = _userModel.value
        if (currentUser != null) {
            _userModel.value = currentUser.copy(name = name)
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

    @DefaultArgumentInterop.Enabled
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
            val credential = EmailAuthProvider.credential(email, password)
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

    @DefaultArgumentInterop.Enabled
    fun createOrSignInUserAndNavigateToHome(
        user: FirebaseUserSmallData,
        name: String? = null,
        signInMethod: SignInMethod? = null,
        appleId: String? = null,
        navToHome: Boolean = true,
        guestGame: Game? = null,
        onErrorMessage: (String?, Boolean) -> Unit,
        onClearGuestGame: (Game?) -> Unit,
        completion: () -> Unit = {}
    ) {
        onErrorMessage(null, false)
        coroutineScope.launch {
            try {
                // Wait for UserRepository to resolve the user
                val loadedUser = userRepository.loadOrCreateUser(
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
                    onClearGuestGame(null)
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

    @DefaultArgumentInterop.Enabled
    fun signUpUIManage(
        emailInput: String,
        passwordInput: String,
        guestGame: Game? = null,
        onClearForm: () -> Unit,
        onSuccess: () -> Unit,
        onErrorMessage: (String?, Boolean) -> Unit,
        onClearGuestGame: (Game?) -> Unit
    ) {
        coroutineScope.launch {
            val result = createUser(emailInput, passwordInput)

            if (result.isFailure) {
                onErrorMessage(result.exceptionOrNull()?.message ?: "Sign up failed", false)
                return@launch
            }

            val firebaseUser = result.getOrNull()
            if (firebaseUser != null) {
                val userData = FirebaseUserSmallData(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    displayName = firebaseUser.displayName,
                    photoURL = null // Safe access: avoiding crash due to GitLive Firebase SDK interop issue on iOS
                )

                createOrSignInUserAndNavigateToHome(
                    user = userData,
                    signInMethod = SignInMethod.EMAIL,
                    navToHome = false,
                    guestGame = guestGame,
                    onErrorMessage = onErrorMessage,
                    onClearGuestGame = onClearGuestGame
                ) {
                    coroutineScope.launch {
                        val verificationResult = authRepository.sendEmailVerification()

                        if (verificationResult.isFailure) {
                            val error = verificationResult.exceptionOrNull()?.message ?: "Unknown error"
                            onErrorMessage("Couldn’t send verification email: $error", false)
                        } else {
                            onClearForm()
                            onSuccess()
                            onErrorMessage("Please Verify Your Email To Continue", true)
                            logout()
                        }
                    }
                }
            }
        }
    }

    fun handleSignInResult(
        result: Result<FirebaseUser?>,
        onErrorMessage: (String?, Boolean) -> Unit,
        onClearGuestGame: (Game?) -> Unit
    ) {
        result.fold(
            onSuccess = { firebaseUser ->
                if (firebaseUser != null) {
                    val userData = FirebaseUserSmallData(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        photoURL = null // Safe access: avoiding crash due to GitLive Firebase SDK interop issue on iOS
                    )
                    createOrSignInUserAndNavigateToHome(
                        user = userData,
                        signInMethod = SignInMethod.GOOGLE,
                        onErrorMessage = onErrorMessage,
                        onClearGuestGame = onClearGuestGame
                    )
                } else {
                    onErrorMessage("User data is missing.", true)
                }
            },
            onFailure = { error ->
                onErrorMessage(error.message ?: "An unknown error occurred", true)
            }
        )
    }

    @DefaultArgumentInterop.Enabled
    fun handleAppleSignInResult(
        result: Result<FirebaseUser?>,
        name: String? = null,
        appleId: String? = null,
        guestGame: Game? = null,
        onErrorMessage: (String?, Boolean) -> Unit,
        onClearGuestGame: (Game?) -> Unit
    ) {
        result.fold(
            onSuccess = { firebaseUser ->
                if (firebaseUser != null) {
                    val userData = FirebaseUserSmallData(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        photoURL = null // Safe access: avoiding crash due to GitLive Firebase SDK interop issue on iOS
                    )
                    createOrSignInUserAndNavigateToHome(
                        user = userData,
                        name = name,
                        signInMethod = SignInMethod.APPLE,
                        appleId = appleId,
                        guestGame = guestGame,
                        onErrorMessage = onErrorMessage,
                        onClearGuestGame = onClearGuestGame
                    )
                } else {
                    onErrorMessage("User data is missing.", true)
                }
            },
            onFailure = { error ->
                onErrorMessage(error.message ?: "An unknown error occurred", true)
            }
        )
    }

    @DefaultArgumentInterop.Enabled
    fun signInUIManage(
        emailInput: String,
        passwordInput: String,
        guestGame: Game? = null,
        onClearForm: () -> Unit,
        onShowSignUp: () -> Unit,
        onErrorMessage: (String?, Boolean) -> Unit,
        onClearGuestGame: (Game?) -> Unit
    ) {
        coroutineScope.launch {
            val result = signIn(emailInput, passwordInput)

            if (result.isFailure) {
                onShowSignUp()
                onErrorMessage("No User Found Please Sign Up", false)
                return@launch
            }
            
            val firebaseUser = result.getOrNull()
            if (firebaseUser != null) {
                if (firebaseUser.isEmailVerified) {

                    val userData = FirebaseUserSmallData(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email,
                        displayName = firebaseUser.displayName,
                        photoURL = null // Safe access: avoiding crash due to GitLive Firebase SDK interop issue on iOS
                    )

                    createOrSignInUserAndNavigateToHome(
                        user = userData,
                        signInMethod = SignInMethod.EMAIL,
                        guestGame = guestGame,
                        onErrorMessage = onErrorMessage,
                        onClearGuestGame = onClearGuestGame
                    ){}
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
                }
            }
        }
    }
}

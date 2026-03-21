@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.garrettbutchko.minimate.viewModels

import co.touchlab.kermit.Logger
import com.garrettbutchko.minimate.datamodels.UserModel
import com.garrettbutchko.minimate.utilities.randomNonceString
import com.garrettbutchko.minimate.utilities.shortHash
import dev.gitlive.firebase.auth.OAuthProvider
import dev.gitlive.firebase.storage.Data
import kotlinx.coroutines.launch
import platform.AuthenticationServices.*
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import com.garrettbutchko.minimate.auth.AppleReauthCoordinator

private var activeReauthCoordinator: AppleReauthCoordinator? = null
private var currentNonce: String? = null
private val log = Logger.withTag("ProfileViewModelIOS")


fun ProfileViewModel.startAppleReauthAndDelete(onSheetPresentChange: (Boolean) -> Unit) {
    val provider = ASAuthorizationAppleIDProvider()
    val request = provider.createRequest()
    request.requestedScopes = listOf<ASAuthorizationScope>()

    val nonce = randomNonceString()
    currentNonce = nonce
    // Apple requires SHA256 of the nonce. We use shortHash as a fallback if full SHA256 isn't available.
    request.nonce = shortHash(nonce)

    val coordinator = AppleReauthCoordinator { result ->
        activeReauthCoordinator = null // Clear strong reference
        result.onSuccess { authorization ->
            deleteAppleAccount(authorization) { deletionSuccess ->
                if (deletionSuccess) {
                    onSheetPresentChange(false)
                    viewManager.navigateToWelcome()
                }
            }
        }.onFailure { error ->
            setBotMessage(error.message ?: "Authorization failed")
            setIsRed(true)
        }
    }
    
    activeReauthCoordinator = coordinator // Retain the coordinator

    val controller = ASAuthorizationController(listOf(request))
    controller.delegate = coordinator
    controller.presentationContextProvider = coordinator
    controller.performRequests()
}

private fun ProfileViewModel.deleteAppleAccount(
    authorization: ASAuthorization,
    completion: (Boolean) -> Unit
) {
    val appleCred = authorization.credential as? ASAuthorizationAppleIDCredential
    val nonce = currentNonce
    val tokenData = appleCred?.identityToken
    
    if (appleCred == null || nonce == null || tokenData == null) {
        setBotMessage("Invalid Apple credential")
        setIsRed(true)
        completion(false)
        return
    }

    val idToken = NSString.create(data = tokenData, encoding = NSUTF8StringEncoding) as String?
    if (idToken == null) {
        setBotMessage("Could not parse identity token")
        setIsRed(true)
        completion(false)
        return
    }

    val oauthCred = OAuthProvider.credential(providerId = "apple.com", idToken = idToken, accessToken = null, rawNonce = nonce)

    coroutineScope.launch {
        val result = authRepository.deleteAccount(oauthCred)
        if (result.isSuccess) {
            cleanupLocalData()
            val userModel = authModel.userModel.value
            if (userModel != null) {
                val model = UserModel(
                    googleId = userModel.googleId,
                    appleId = userModel.appleId,
                    name = userModel.name,
                    photoURL = null,
                    email = userModel.email,
                    gameIDs = listOf(),
                    accountType = listOf("apple")
                )
                userRemoteRepo.save(model)
            }
            completion(true)
        } else {
            setBotMessage(result.exceptionOrNull()?.message ?: "Deletion failed")
            setIsRed(true)
            completion(false)
        }
    }
}

fun ProfileViewModel.managePictureChange(newImage: UIImage?) {
    val userId = authModel.currentUserIdentifier
    if (newImage == null || userId == null) {
        setBotMessage("Photo upload failed: Missing image or user ID")
        setIsRed(true)
        return
    }

    val jpegData = UIImageJPEGRepresentation(newImage, 0.8)
    if (jpegData == null) {
        setBotMessage("Photo upload failed: Could not process image")
        setIsRed(true)
        return
    }

    val data = Data(jpegData)

    coroutineScope.launch {
        val result = userRepo.uploadProfilePhoto(userId, data)
        if (result.isSuccess) {
            log.i { "✅ Photo URL: ${result.getOrNull()}" }
        } else {
            setBotMessage("Photo upload failed: ${result.exceptionOrNull()?.message}")
            setIsRed(true)
        }
    }
}

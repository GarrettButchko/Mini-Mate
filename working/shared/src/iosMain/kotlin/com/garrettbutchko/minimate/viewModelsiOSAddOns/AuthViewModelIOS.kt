@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.garrettbutchko.minimate.viewModelsiOSAddOns

import com.garrettbutchko.minimate.utilities.randomNonceString
import com.garrettbutchko.minimate.utilities.shortHash
import com.garrettbutchko.minimate.viewModels.AuthViewModel
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.OAuthProvider
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.launch
import platform.AuthenticationServices.*
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create

fun AuthViewModel.handleSignInWithAppleRequest(request: ASAuthorizationAppleIDRequest) {
    request.requestedScopes = listOf<ASAuthorizationScope>() // usually ASAuthorizationScopeFullName, ASAuthorizationScopeEmail
    val nonce = randomNonceString()
    currentNonce = nonce
    request.nonce = shortHash(nonce)
}

fun AuthViewModel.signInWithApple(
    authorization: ASAuthorization,
    completion: (Result<FirebaseUser?>) -> Unit
) {
    val appleCred = authorization.credential as? ASAuthorizationAppleIDCredential
    val nonce = currentNonce
    val tokenData = appleCred?.identityToken
    
    if (appleCred == null || nonce == null || tokenData == null) {
        completion(Result.failure(Exception("Invalid Apple credential")))
        return
    }

    val idToken = NSString.create(data = tokenData, encoding = NSUTF8StringEncoding)?.toString()
    if (idToken == null) {
        completion(Result.failure(Exception("Could not parse identity token")))
        return
    }

    val accessTokenData = appleCred.authorizationCode
    val accessToken = if (accessTokenData != null) {
        NSString.create(data = accessTokenData, encoding = NSUTF8StringEncoding)?.toString()
    } else null

    val oauthCred = OAuthProvider.credential(
        providerId = "apple.com",
        idToken = idToken,
        accessToken = accessToken,
        rawNonce = nonce
    )

    coroutineScope.launch {
        val result = authRepository.signInWithCredential(oauthCred)
        if (result.isSuccess) {
            val user = result.getOrNull()
            refreshUID()
            completion(Result.success(user))
        } else {
            completion(Result.failure(result.exceptionOrNull() ?: Exception("Unknown error during sign in")))
        }
    }
}

fun AuthViewModel.deleteAppleAccount(
    authorization: ASAuthorization,
    completion: (Result<Unit>) -> Unit
) {
    val appleCred = authorization.credential as? ASAuthorizationAppleIDCredential
    val nonce = currentNonce
    val tokenData = appleCred?.identityToken
    
    if (appleCred == null || nonce == null || tokenData == null) {
        completion(Result.failure(Exception("Invalid Apple credential")))
        return
    }

    val idToken = NSString.create(data = tokenData, encoding = NSUTF8StringEncoding)?.toString()
    if (idToken == null) {
        completion(Result.failure(Exception("Could not parse identity token")))
        return
    }

    val oauthCred = OAuthProvider.credential(
        providerId = "apple.com",
        idToken = idToken,
        accessToken = null,
        rawNonce = nonce
    )

    coroutineScope.launch {
        val result = deleteAccount(oauthCred)
        completion(result)
    }
}

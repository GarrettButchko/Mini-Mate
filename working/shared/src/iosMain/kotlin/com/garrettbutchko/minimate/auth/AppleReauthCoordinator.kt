package com.garrettbutchko.minimate.auth


import platform.AuthenticationServices.*
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject


class AppleReauthCoordinator(
    val onAuthorize: (Result<ASAuthorization>) -> Unit
) : NSObject(), ASAuthorizationControllerDelegateProtocol, ASAuthorizationControllerPresentationContextProvidingProtocol {

    override fun presentationAnchorForAuthorizationController(controller: ASAuthorizationController): ASPresentationAnchor? {
        val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull() as? UIWindowScene
        return scene?.windows?.firstOrNull() as? UIWindow
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithAuthorization: ASAuthorization
    ) {
        onAuthorize(Result.success(didCompleteWithAuthorization))
    }

    override fun authorizationController(
        controller: ASAuthorizationController,
        didCompleteWithError: NSError
    ) {
        onAuthorize(Result.failure(Exception(didCompleteWithError.localizedDescription)))
    }
}
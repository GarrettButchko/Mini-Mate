//
//  AuthViewModelGoogle.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/20/26.
//
import Foundation
import FirebaseAuth
import FirebaseDatabase
import GoogleSignIn
import FirebaseCore
import SwiftData
import AuthenticationServices
import SwiftUI
import Combine

#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif

/// Signs in the user using Google Sign-In and Firebase via KMP
func signInWithGoogle(authModel: AuthViewModel, completion: @escaping (Result<Firebase_authFirebaseUser?, Error>) -> Void) {
    guard let clientID = FirebaseApp.app()?.options.clientID else {
        completion(.failure(NSError(domain: "AuthViewModel", code: -1, userInfo: [NSLocalizedDescriptionKey: "Missing Firebase client ID"])))
        return
    }
    let config = GIDConfiguration(clientID: clientID)
    GIDSignIn.sharedInstance.configuration = config
    
    guard let rootVC = UIApplication.shared.connectedScenes
        .compactMap({ ($0 as? UIWindowScene)?.windows.first?.rootViewController })
        .first else {
        completion(.failure(NSError(domain: "AuthViewModel", code: -1, userInfo: [NSLocalizedDescriptionKey: "Unable to access rootViewController"])))
        return
    }
    
    GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { signInResult, error in
        if let error = error {
            completion(.failure(error)); return
        }
        guard let user = signInResult?.user,
              let idToken = user.idToken?.tokenString else {
            completion(.failure(NSError(domain: "AuthViewModel", code: -1, userInfo: [NSLocalizedDescriptionKey: "Google ID token missing"])))
            return
        }
        
        
        // Call our new shared Kotlin method!
        authModel.signInWithGoogleTokens(idToken: idToken, accessToken: user.accessToken.tokenString) { result in
            completion(result as! Result<Firebase_authFirebaseUser?, any Error>)
        }
    }
}

/// Reauthenticate a Google user and hand back the KMP `AuthCredential`
func reauthenticateWithGoogle(authModel: AuthViewModel, completion: @escaping (Result<AuthCredential, Error>) -> Void) {
    guard let clientID = FirebaseApp.app()?.options.clientID else {
        completion(.failure(NSError(
            domain: "AuthViewModel",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "Missing Firebase client ID"]
        )))
        return
    }
    
    let config = GIDConfiguration(clientID: clientID)
    GIDSignIn.sharedInstance.configuration = config
    
    guard let rootVC = UIApplication.shared.connectedScenes
        .compactMap({ ($0 as? UIWindowScene)?.windows.first?.rootViewController })
        .first
    else {
        completion(.failure(NSError(
            domain: "AuthViewModel",
            code: -1,
            userInfo: [NSLocalizedDescriptionKey: "Unable to access rootViewController"]
        )))
        return
    }
    
    GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { signInResult, error in
        if let error = error {
            completion(.failure(error))
            return
        }
        guard
            let user    = signInResult?.user,
            let idToken = user.idToken?.tokenString
        else {
            completion(.failure(NSError(
                domain: "AuthViewModel",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Google re-authentication failed"]
            )))
            return
        }
        
        let accessToken = user.accessToken.tokenString
        
        // Call our new shared Kotlin method to convert to a GitLive KMP AuthCredential
        authModel.getGoogleCredential(idToken: idToken, accessToken: accessToken) { result in
            completion(result as! Result<AuthCredential, any Error>)
        }
    }
}

//
//  AuthViewModelSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/20/26.
//

import SwiftUI
import Combine
import FirebaseAuth
import Foundation
import FirebaseDatabase
import GoogleSignIn
import FirebaseCore
import SwiftData
import AuthenticationServices

#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif

@MainActor
class AuthViewModelSwift: ObservableObject {
    private let kotlinVM: AuthViewModel
    
    // 1. Internal storage for the observed Flow values
    @Published private var _firebaseUser: Firebase_authFirebaseUser?
    @Published private var _userModel: UserModel?
    @Published private var _isLoading: Bool = false

    // 2. Public Computed Properties with Getters and Setters
    var userModel: UserModel? {
        get { _userModel }
        set { kotlinVM.setUserModel(userModel: newValue) }
    }

    var firebaseUser: Firebase_authFirebaseUser? {
        get { _firebaseUser }
        set { kotlinVM.refreshUID() }
    }

    var isLoading: Bool {
        get { _isLoading }
        set { kotlinVM.setLoading(state: newValue) }
    }

    init() {
        self.kotlinVM = KoinHelperParent.shared.getAuthViewModel()
        setupObservations()
    }

    private func setupObservations() {
        Task {
            for await user in kotlinVM.firebaseUser {
                self._firebaseUser = user
            }
        }
        
        Task {
            for await model in kotlinVM.userModel {
                self._userModel = model
            }
        }
        
        Task {
            for await loading in kotlinVM.isLoadingUser {
                self._isLoading = loading.boolValue
            }
        }
    }
    
    
    /// Signs in the user using Google Sign-In and Firebase via KMP
    func signInWithGoogle(completion: @escaping (Result<Firebase_authFirebaseUser?, Error>) -> Void) {
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
            Task { @MainActor in
                if let error = error {
                    completion(.failure(error)); return
                }
                guard let user = signInResult?.user,
                      let idToken = user.idToken?.tokenString else {
                    completion(.failure(NSError(domain: "AuthViewModel", code: -1, userInfo: [NSLocalizedDescriptionKey: "Google ID token missing"])))
                    return
                }
                
                self.kotlinVM.signInWithGoogleTokens(idToken: idToken, accessToken: user.accessToken.tokenString) { result in
                    completion(result as! Result<Firebase_authFirebaseUser?, any Error>)
                }
            }
        }
    }

    /// Reauthenticate a Google user and hand back the KMP `AuthCredential`
    func reauthenticateWithGoogle(completion: @escaping (Result<AuthCredential, Error>) -> Void) {
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
            Task { @MainActor in
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
                self.kotlinVM.getGoogleCredential(idToken: idToken, accessToken: accessToken) { result in
                    completion(result as! Result<AuthCredential, any Error>)
                }
            }
        }
    }
}

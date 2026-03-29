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

#if MINIMATE
import shared_user
#elseif MANAGER
import shared_admin
#endif

@MainActor
class AuthViewModelSwift: ObservableObject {
    let kotlinVM: AuthViewModel
    
    // 1. Internal storage for the observed Flow values
    @Published private var _firebaseUser: Firebase_authFirebaseUser?
    @Published private var _userModel: UserModel?
    @Published private var _isLoading: Bool = false

    // 2. Public Computed Properties with Getters and Setters
    var userModel: UserModel? {
        get { _userModel }
        set { 
            // Update local state immediately to avoid race conditions with Flow emission
            self._userModel = newValue
            kotlinVM.setUserModel(userModel: newValue) 
        }
    }

    var firebaseUser: Firebase_authFirebaseUser? {
        get { _firebaseUser }
        set { kotlinVM.refreshUID() }
    }

    var isLoading: Bool {
        get { _isLoading }
        set { 
            self._isLoading = newValue
            kotlinVM.setLoading(state: newValue) 
        }
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
                // Only update if it's actually different to avoid overriding immediate local changes
                if self._userModel != model {
                    self._userModel = model
                }
            }
        }
        
        Task {
            for await loading in kotlinVM.isLoadingUser {
                self._isLoading = loading.boolValue
            }
        }
    }
    
    
    func createOrSignInUserAndNavigateToHome(
        user: FirebaseUserSmallData,
        name: String? = nil,
        errorMessage: Binding<(message: String?,type: Bool)>,
        signInMethod: SignInMethod? = nil,
        appleId: String? = nil,
        navToHome: Bool = true,
        guestGame: Binding<Game?>,
        completion: @escaping(() -> Void)){
            kotlinVM.createOrSignInUserAndNavigateToHome(
                user: user,
                name: name,
                signInMethod: signInMethod,
                appleId: appleId,
                navToHome: navToHome,
                guestGame: guestGame.wrappedValue) { message, type in
                    errorMessage.wrappedValue = (message, type.boolValue)
                } onClearGuestGame: { game in
                    guestGame.wrappedValue = game
                } completion: {
                    completion()
                }
    }

    func signInUIManage(
        email: Binding<String>,
        password: Binding<String>,
        confirmPassword: Binding<String>,
        isTextFieldFocused: FocusState<SignInView.Field?>.Binding,
        errorMessage: Binding<(message: String?, type: Bool)>,
        showSignUp: Binding<Bool>,
        guestGame: Binding<Game?>){
        
        kotlinVM.signInUIManage(emailInput: email.wrappedValue, passwordInput: password.wrappedValue, guestGame: guestGame.wrappedValue) {
            email.wrappedValue = ""
            password.wrappedValue  = ""
            confirmPassword.wrappedValue  = ""
            isTextFieldFocused.wrappedValue = nil
        } onShowSignUp: {
            showSignUp.wrappedValue = true
        } onErrorMessage: { message, type in
            errorMessage.wrappedValue = (message, type.boolValue)
        } onClearGuestGame: { game in
            guestGame.wrappedValue = game
        }
    }

    func signUpUIManage(
        email: Binding<String>,
        password: Binding<String>,
        confirmPassword: Binding<String>,
        isTextFieldFocused: FocusState<SignInView.Field?>.Binding,
        errorMessage: Binding<(message: String?, type: Bool)>,
        showSignUp: Binding<Bool>,
        guestGame: Binding<Game?>
    ) {
        kotlinVM.signUpUIManage(
            emailInput: email.wrappedValue,
            passwordInput: password.wrappedValue,
            guestGame: guestGame.wrappedValue,
            onClearForm: {
                email.wrappedValue = ""
                password.wrappedValue = ""
                confirmPassword.wrappedValue = ""
                isTextFieldFocused.wrappedValue = nil
            },
            onSuccess: {
                showSignUp.wrappedValue = false
            },
            onErrorMessage: { message, type in
                errorMessage.wrappedValue = (message, type.boolValue)
            },
            onClearGuestGame: { game in
                guestGame.wrappedValue = game
            }
        )
    }
    
    
    /// Signs in the user using Google Sign-In and Firebase via KMP
    func signInWithGoogle(completion: @escaping (Any?) -> Void) {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            completion(nil)
            return
        }
        let config = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.configuration = config
        
        guard let rootVC = UIApplication.shared.connectedScenes
            .compactMap({ ($0 as? UIWindowScene)?.windows.first?.rootViewController })
            .first else {
            completion(nil)
            return
        }
        
        GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { signInResult, error in
            Task { @MainActor in
                if error != nil {
                    completion(nil); return
                }
                guard let user = signInResult?.user,
                      let idToken = user.idToken?.tokenString else {
                    completion(nil)
                    return
                }
                
                self.kotlinVM.signInWithGoogleTokens(idToken: idToken, accessToken: user.accessToken.tokenString) { result in
                    completion(result)
                }
            }
        }
    }

    /// Reauthenticate a Google user and hand back the KMP `AuthCredential`
    func reauthenticateWithGoogle(completion: @escaping (Result<(idToken: String, accessToken: String?), Error>) -> Void) {
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
                completion(.success((idToken: idToken, accessToken: accessToken)))
            }
        }
    }
    
    func signInWithApple(
        result: Result<ASAuthorization, Error>,
        errorMessage: Binding<(message: String?, type: Bool)>,
        guestGame: Binding<Game?>
    ) {
        switch result {
        case .success(let authorization):
            kotlinVM.signInWithApple(
                authorization: authorization,
                guestGame: guestGame.wrappedValue,
                onErrorMessage: { message, type in
                    errorMessage.wrappedValue = (message, type.boolValue)
                },
                onClearGuestGame: { game in
                    guestGame.wrappedValue = game
                }
            )
        case .failure(let error):
            errorMessage.wrappedValue = (error.localizedDescription, true)
        }
    }
}

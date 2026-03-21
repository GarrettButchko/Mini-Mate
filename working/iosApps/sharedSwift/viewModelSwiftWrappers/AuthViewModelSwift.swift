//
//  AuthViewModelSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/20/26.
//

import SwiftUI
import Combine
import FirebaseAuth

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
        self.kotlinVM = KoinHelper.shared.getAuthViewModel()
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
}

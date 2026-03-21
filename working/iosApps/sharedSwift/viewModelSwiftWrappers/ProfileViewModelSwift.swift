//
//  Untitled.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/20/26.
//

import SwiftUI
import Combine

#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif

@MainActor
class ProfileViewModelSwift: ObservableObject {
    private let kotlinVM: ProfileViewModel
    
    // 1. Internal storage for observed Flow values
    @Published private var _editProfile: Bool = false
    @Published private var _botMessage: String = ""
    @Published private var _isRed: Bool = true
    @Published private var _name: String = ""
    @Published private var _email: String = ""
    @Published private var _activeDeleteAlert: DeleteAlertType? = nil

    // 2. Public Computed Properties (Getters and Setters)
    var editProfile: Bool {
        get { _editProfile }
        set { kotlinVM.setEditProfile(value: newValue) }
    }

    var botMessage: String {
        get { _botMessage }
        set { kotlinVM.setBotMessage(value: newValue) }
    }

    var isRed: Bool {
        get { _isRed }
        set { kotlinVM.setIsRed(value: newValue) }
    }

    var name: String {
        get { _name }
        set { kotlinVM.setName(value: newValue) }
    }

    var email: String {
        get { _email }
        set { kotlinVM.setEmail(value: newValue) }
    }

    var activeDeleteAlert: DeleteAlertType? {
        get { _activeDeleteAlert }
        set { kotlinVM.setActiveDeleteAlert(value: newValue) }
    }

    init() {
        // Assuming your KoinHelper has a getter for ProfileViewModel
        
        self.kotlinVM = KoinHelperParent.shared.getProfileViewModel()
        setupObservations()
    }

    // 3. Setup Task-based observations for Kotlin StateFlows
    private func setupObservations() {
        Task {
            for await value in kotlinVM.editProfile {
                self._editProfile = value.boolValue
            }
        }
        
        Task {
            for await value in kotlinVM.botMessage {
                self._botMessage = value
            }
        }
        
        Task {
            for await value in kotlinVM.isRed {
                self._isRed = value.boolValue
            }
        }
        
        Task {
            for await value in kotlinVM.name {
                self._name = value
            }
        }
        
        Task {
            for await value in kotlinVM.email {
                self._email = value
            }
        }
        
        Task {
            for await value in kotlinVM.activeDeleteAlert {
                self._activeDeleteAlert = value
            }
        }
    }
}

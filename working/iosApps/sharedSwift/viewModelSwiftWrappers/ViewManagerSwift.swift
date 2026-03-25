//
//  ViewManagerSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/21/26.
//

import SwiftUI
import Combine

#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif


import Foundation
import shared_user
import FirebaseAuth // Assuming you're using the native Firebase SDK in iOS

@MainActor
class ViewManagerSwift: ObservableObject {
    let kotlinVM: ViewManager
    
    @Published var currentView: ViewType
    
    init() {
        // 1. First, get the instance from your Koin helper
        let vm = KoinHelperParent.shared.getViewManager() as! ViewManager
        self.kotlinVM = vm
        
        // 2. Determine initial state using Swift Firebase logic
        let currentUser = Auth.auth().currentUser
        
        if let user = currentUser, user.isEmailVerified {
            self.currentView = ViewType.Main(tab: 1)
            vm.navigateToMain(tab: 1)
        } else {
            // Logic for signing out if not verified
            if let user = currentUser, !user.isEmailVerified {
                Task {
                    do {
                        try Auth.auth().signOut()
                    } catch {
                        print("Error signing out: \(error)")
                    }
                }
            }
            self.currentView = ViewType.Welcome()
            vm.navigateToWelcome()
        }
    
        setupObservations()
    }
    
    private func setupObservations() {
        Task {
            for await view in kotlinVM.currentView {
                self.currentView = view
            }
        }
    }
}

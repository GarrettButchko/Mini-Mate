//
//  ViewManagerSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/21/26.
//

import SwiftUI
import Combine
import Foundation
import FirebaseAuth

#if MINIMATE
import shared_user
#elseif MANAGER
import shared_admin
#endif

@MainActor
class ViewManagerSwift: ObservableObject {
    let kotlinVM: ViewManager
    
    @Published var currentView: ViewType
    
    init() {
        // 1. Get the instance from Koin
        let vm = KoinHelperParent.shared.getViewManager() as! ViewManager
        self.kotlinVM = vm
        
        // 2. Set the initial Swift state to whatever Kotlin determined in its init
        self.currentView = vm.currentView.value
        
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

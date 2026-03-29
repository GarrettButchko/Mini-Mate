//
//  WelcomeViewModelSwift.swift
//  sharedSwift
//
//  Created by Garrett Butchko on 3/21/26.
//

import SwiftUI
import Combine
import Foundation

#if MINIMATE
import shared_user
#elseif MANAGER
import shared_admin
#endif

@MainActor
class WelcomeViewModelSwift: ObservableObject {
    let kotlinVM: WelcomeViewModel
    
    @Published var displayedText: String = ""
    @Published var showLoading: Bool = false

    init(welcomeText: String) {
        self.kotlinVM = KoinHelperParent.shared.getWelcomeViewModel(welcomeText: welcomeText)
        setupObservations()
    }

    private func setupObservations() {
        Task {
            for await text in kotlinVM.displayedText {
                self.displayedText = text
            }
        }
        
        Task {
            for await loading in kotlinVM.showLoading {
                self.showLoading = loading.boolValue
            }
        }
    }
}

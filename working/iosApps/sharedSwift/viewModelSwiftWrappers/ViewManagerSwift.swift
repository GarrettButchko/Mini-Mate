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


@MainActor
class ViewManagerSwift: ObservableObject {
    private let kotlinVM: ViewManager
    
    // 1. Internal storage for the observed Flow values
    @Published private var _currentView: ViewType = ViewType.Welcome.shared
    
    var currentView: ViewType {
        get { _currentView }
        set { kotlinVM.setCurrentView(view: newValue) }
    }
    
    init() {
        // Assuming you have a KoinHelper or similar provider for ViewManager
        self.kotlinVM = KoinHelperParent.shared.getViewManager() as! ViewManager
        setupObservations()
    }
    
    private func setupObservations() {
        Task {
            // Observe the Kotlin StateFlow
            for await view in kotlinVM.currentView {
                self._currentView = view
            }
        }
    }
}

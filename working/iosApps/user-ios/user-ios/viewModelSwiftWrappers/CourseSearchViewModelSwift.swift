//
//  CourseSearchViewModelSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/22/26.
//


import SwiftUI
import Combine
import shared_user

@MainActor
class CourseSearchViewModelSwift: ObservableObject {
    let kotlin: CourseSearchViewModel
    
    // 1. Observed properties from Kotlin Flows
    @Published var mapCameraPosition: MapRegionData?
    @Published var selectedMapItem: MapItemDTO?
    @Published var mapItems: [MapItemDTO] = []
    @Published var nameExists: [String: Bool] = [:]
    @Published var isSearchPanelVisible: Bool = false
    @Published var hasLocationAccess: Bool = false
    
    // 2. Public Getters/Setters that sync back to Kotlin
    var cameraPosition: MapRegionData? {
        get { mapCameraPosition }
        set { kotlin.setMapCameraPosition(position: newValue) }
    }
    
    var selectedItem: MapItemDTO? {
        get { selectedMapItem }
        set { kotlin.setSelectedMapItem(item: newValue) }
    }
    
    var searchPanelVisible: Bool {
        get { isSearchPanelVisible }
        set { kotlin.setSearchPanelVisible(isVisible: newValue) }
    }

    init() {
        // Assuming your KoinHelper has a getter for this ViewModel
        self.kotlin = KoinHelper.shared.getCourseSearchViewModel()
        setupObservations()
    }

    private func setupObservations() {
        // Observe Camera Position
        Task {
            for await position in kotlin.mapCameraPosition {
                self.mapCameraPosition = position
            }
        }
        
        // Observe Selected Item
        Task {
            for await item in kotlin.selectedMapItem {
                self.selectedMapItem = item
            }
        }
        
        // Observe Map Items List
        Task {
            for await items in kotlin.mapItems {
                self.mapItems = items
            }
        }
        
        // Observe Name Exists Map (Manual conversion for KotlinBoolean)
        Task {
            for await mapValue in kotlin.nameExists {
                self.nameExists = mapValue.mapValues { $0.boolValue }
            }
        }
        
        // Observe Search Panel Visibility
        Task {
            for await visible in kotlin.isSearchPanelVisible {
                self.isSearchPanelVisible = visible.boolValue
            }
        }
        
        // Observe Location Access
        Task {
            for await access in kotlin.hasLocationAccess {
                self.hasLocationAccess = access.boolValue
            }
        }
    }
    
    // 3. Forwarding Methods
    func onAppear() {
        kotlin.onAppear()
    }
    
    func recenterMap() {
        kotlin.recenterMap()
    }
}

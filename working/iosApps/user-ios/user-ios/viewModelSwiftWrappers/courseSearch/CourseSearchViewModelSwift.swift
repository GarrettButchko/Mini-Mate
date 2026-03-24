//
//  CourseSearchViewModelSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/22/26.
//


import SwiftUI
import Combine
import shared_user
import MapKit

@MainActor
class CourseSearchViewModelSwift: ObservableObject {
    let kotlin: CourseSearchViewModel
    
    // 1. Observed properties from Kotlin Flows
    @Published var mapCameraPosition: MapCameraPosition = .automatic
    @Published var selectedMapItem: MKMapItem?
    @Published var mapItems: [MKMapItem] = []
    @Published var nameExists: [String: Bool] = [:]
    @Published var isSearchPanelVisible: Bool = false
    @Published var hasLocationAccess: Bool = false
    
    // 2. Public Getters/Setters that sync back to Kotlin
    var cameraPosition: MapCameraPosition? {
        get { mapCameraPosition }
        set { kotlin.setMapCameraPosition(position: newValue?.region?.toMapRegionData()) }
    }
    
    var selectedItem: MKMapItem? {
        get { selectedMapItem }
        set { kotlin.setSelectedMapItem(item: newValue?.toDTO()) }
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
                if let cameraPosition = position?.toMKCoordinateRegion() {
                    self.mapCameraPosition = .region(cameraPosition)
                }
            }
        }
        
        // Observe Selected Item
        Task {
            for await item in kotlin.selectedMapItem {
                self.selectedMapItem = item?.toMKMapItem()
            }
        }
        
        // Observe Map Items List
        Task {
            for await items in kotlin.mapItems {
                self.mapItems = items.map({ $0.toMKMapItem() })
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
}

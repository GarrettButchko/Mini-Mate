//
//  LocationHandlerSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/21/26.
//


import SwiftUI
import Combine
import CoreLocation
import MapKit
import shared_user

@MainActor
class LocationHandlerSwift: ObservableObject {
    let kotlin: LocationHandler
    
    // 1. Observed properties from Kotlin Flows
    @Published private var _mapItems: [MKMapItem] = []
    @Published private var _selectedItem: MKMapItem? = nil
    @Published private var _userLocation: CLLocation? = nil
    @Published private var _hasLocationAccess: Bool = false
    
    var mapItems: [MKMapItem] {
        get { _mapItems }
        set { kotlin.setMapItems(items: newValue.map( {$0.toDTO()} ))}
    }
    
    var selectedItem: MKMapItem? {
        get { _selectedItem }
        set { kotlin.setSelectedItem(item: newValue?.toDTO()) }
    }
    
    var userLocation: CLLocation? {
        get { _userLocation }
        set { kotlin.setUserLocation(location: newValue?.toDTO()) }
    }
    
    var hasLocationAccess: Bool {
        get { _hasLocationAccess }
        set { kotlin.setHasLocationAccess(hasAccess: newValue) }
    }

    init() {
        // Since LocationHandler is a class, we instantiate it directly
        self.kotlin = LocationHandler()
        setupObservations()
    }

    func requestLocationAccess() {
        kotlin.requestLocationAccess()
    }

    private func setupObservations() {
        // Observe search results
        Task {
            for await items in kotlin.mapItems {
                self._mapItems = items.map ({ $0.toMKMapItem() })
            }
        }

        // Observe the selected course
        Task {
            for await item in kotlin.selectedItem {
                self._selectedItem = item?.toMKMapItem()
            }
        }

        // Observe user location updates
        Task {
            for await location in kotlin.userLocation {
                self._userLocation = location?.toCLLocation()
            }
        }

        // Observe permission status
        Task {
            for await access in kotlin.hasLocationAccess {
                self._hasLocationAccess = access.boolValue
            }
        }
    }
}

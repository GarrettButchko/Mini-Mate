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
        set { kotlin.setMapItems(items: newValue) }
    }
    
    var selectedItem: MKMapItem? {
        get { _selectedItem }
        set { kotlin.setSelectedItem(item: newValue) }
    }
    
    var userLocation: CLLocation? {
        get { _userLocation }
        set { kotlin.setUserLocation(location: newValue) }
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

    private func setupObservations() {
        // Observe search results
        Task {
            for await items in kotlin.mapItems {
                self._mapItems = items
            }
        }

        // Observe the selected course
        Task {
            for await item in kotlin.selectedItem {
                self._selectedItem = item
            }
        }

        // Observe user location updates
        Task {
            for await location in kotlin.userLocation {
                self._userLocation = location
            }
        }

        // Observe permission status
        Task {
            for await access in kotlin.hasLocationAccess {
                self._hasLocationAccess = access.boolValue
            }
        }
    }
    
    func searchNearbyCourses(
        upwardOffset: CLLocationDegrees = 0.03,
        span: MKCoordinateSpan = MKCoordinateSpan(latitudeDelta: 0.1, longitudeDelta: 0.1),
        completion: @escaping (Bool, MapCameraPosition?) -> Void
    ) {
        kotlin.searchNearbyCourses(
            upwardOffset: upwardOffset,
            latitudeDelta: span.latitudeDelta,
            longitudeDelta: span.longitudeDelta
        ) { kBool, kMKCoordinateRegion in
            
            // 1. Basic safety checks
            guard kBool.boolValue, let genericValue = kMKCoordinateRegion else {
                completion(false, nil)
                return
            }
            
            let center = CLLocationCoordinate2D(latitude: genericValue.latitude, longitude: genericValue.longitude)
            let span = MKCoordinateSpan(latitudeDelta: genericValue.latitudeDelta, longitudeDelta: genericValue.longitudeDelta)
            
            let newRegion = MKCoordinateRegion(center: center, span: span)
            let cameraPosition: MapCameraPosition = .region(newRegion)
            
            completion(true, cameraPosition)
        }
    }
}

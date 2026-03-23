//
//  CourseViewModelSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/21/26.
//

import SwiftUI
import Combine
import shared_user
import MapKit

@MainActor
class CourseViewModelSwift: ObservableObject {
    let kotlin: CourseViewModel
    
    // 1. Observed properties from Kotlin Flows
    @Published private var _nameExists: [String: Bool] = [:]
    @Published private var _position: MapCameraPosition = .automatic
    @Published private var _isUpperHalf: Bool = false
    @Published private var _hasAppeared: Bool = false
    @Published private var _isLoadingCourses: Bool = false
    @Published private var _selectedCourse: Course? = nil
    
    // 2. Public Computed Properties with Getters and Setters
    var nameExists: [String: Bool] {
        get { _nameExists }
        set {
            let kotlinMappedDict = newValue.mapValues { KotlinBoolean(bool: $0) }
            kotlin.setNameExists(value: kotlinMappedDict)
        }
    }
    
    var position: MapCameraPosition {
        get { _position }
        set {
            if let region = newValue.region {
                kotlin.setPosition(position: region.toMapRegionData())
            }
        }
    }
    
    var isUpperHalf: Bool {
        get { _isUpperHalf }
    }
    
    var hasAppeared: Bool {
        get { _hasAppeared }
    }
    
    var isLoadingCourses: Bool {
        get { _isLoadingCourses }
    }
    
    var selectedCourse: Course? {
        get { _selectedCourse }
        set { kotlin.setCourse(course: newValue) }
    }
    
    init() {
        self.kotlin = KoinHelper.shared.getCourseViewModel()
        setupObservations()
    }

    private func setupObservations() {
        // Observe name exists map
        Task {
            for await map in kotlin.nameExists {
                let swiftMap = map.mapValues { $0.boolValue }
                self._nameExists = swiftMap
            }
        }
        
        // Observe map position updates
        Task {
            for await pos in kotlin.position {
                if let posData = pos {
                    self._position = .region(posData.toMKCoordinateRegion())
                }
            }
        }
        
        // Observe upper half state
        Task {
            for await upper in kotlin.isUpperHalf {
                self._isUpperHalf = upper.boolValue
            }
        }
        
        // Observe appearance state
        Task {
            for await appeared in kotlin.hasAppeared {
                self._hasAppeared = appeared.boolValue
            }
        }
        
        // Observe loading courses state
        Task {
            for await loading in kotlin.isLoadingCourses {
                self._isLoadingCourses = loading.boolValue
            }
        }
        
        // Observe selected course
        Task {
            for await course in kotlin.selectedCourse {
                self._selectedCourse = course
            }
        }
    }
}

extension MKCoordinateRegion {
    func toMapRegionData() -> MapRegionData {
        return MapRegionData(
            latitude: self.center.latitude,
            longitude: self.center.longitude,
            latitudeDelta: self.span.latitudeDelta,
            longitudeDelta: self.span.longitudeDelta
        )
    }
}

extension MapRegionData {
    func toMKCoordinateRegion() -> MKCoordinateRegion {
        return MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: self.latitude, longitude: self.longitude),
            span: MKCoordinateSpan(latitudeDelta: self.latitudeDelta, longitudeDelta: self.longitudeDelta)
        )
    }
}

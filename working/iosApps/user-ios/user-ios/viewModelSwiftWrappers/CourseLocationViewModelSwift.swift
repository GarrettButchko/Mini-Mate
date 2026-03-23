//
//  CourseLocationViewModelSwift.swift
//  user-ios
//
//  Created by MiniMate on 2/28/25.
//

import SwiftUI
import Combine
import shared_user

@MainActor
class CourseLocationViewModelSwift: ObservableObject {
    let kotlin: CourseLocationViewModel
    
    // 1. Observed properties from Kotlin Flows
    @Published private var _courseName: String = ""
    @Published private var _postalAddress: String = ""
    @Published private var _phoneNumber: String? = nil
    @Published private var _phoneNumberURL: String? = nil
    @Published private var _websiteURL: String? = nil
    @Published private var _course: Course? = nil
    @Published private var _isLoading: Bool = false
    
    // 2. Public Computed Properties with Getters and Setters
    var courseName: String {
        get { _courseName }
        set { kotlin.setCourseName(name: newValue) }
    }
    
    var postalAddress: String {
        get { _postalAddress }
        set { kotlin.setPostalAddress(address: newValue) }
    }
    
    var phoneNumber: String? {
        get { _phoneNumber }
        set { kotlin.setPhoneNumber(number: newValue) }
    }
    
    var phoneNumberURL: String? {
        get { _phoneNumberURL }
        set { kotlin.setPhoneNumberURL(url: newValue) }
    }
    
    var websiteURL: String? {
        get { _websiteURL }
        set { kotlin.setWebsiteURL(url: newValue) }
    }
    
    var course: Course? {
        get { _course }
        set { kotlin.setCourse(course: newValue) }
    }
    
    var isLoading: Bool {
        get { _isLoading }
        set { kotlin.setIsLoading(isLoading: newValue) }
    }
    
    // 3. Passthrough Properties for Non-Flow State
    var isCourseSupported: Bool {
        kotlin.isCourseSupported
    }
    
    var socialLinks: [SocialLink] {
        kotlin.socialLinks
    }
    
    init() {
        // NOTE: Make sure `getCourseLocationViewModel()` is exposed in KoinHelper or injected properly!
        self.kotlin = KoinHelper.shared.getCourseLocationViewModel()
        setupObservations()
    }

    private func setupObservations() {
        Task {
            for await value in kotlin.courseName {
                self._courseName = value
            }
        }
        
        Task {
            for await value in kotlin.postalAddress {
                self._postalAddress = value
            }
        }
        
        Task {
            for await value in kotlin.phoneNumber {
                self._phoneNumber = value
            }
        }
        
        Task {
            for await value in kotlin.phoneNumberURL {
                self._phoneNumberURL = value
            }
        }
        
        Task {
            for await value in kotlin.websiteURL {
                self._websiteURL = value
            }
        }
        
        Task {
            for await value in kotlin.course {
                self._course = value
            }
        }
        
        Task {
            for await value in kotlin.isLoading {
                self._isLoading = value.boolValue
            }
        }
    }
}

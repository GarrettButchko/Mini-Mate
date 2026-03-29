//
//  CourseSettingsViewModelSwift.swift
//  admin-ios
//

import SwiftUI
import Combine
import shared_admin

@MainActor
class CourseSettingsViewModelSwift: ObservableObject {
    let kotlin: CourseSettingsViewModel
    
    @Published private var _editCourse: Bool = false
    @Published private var _showingPickerLogo: Bool = false
    @Published private var _showingPickerAd: Bool = false
    @Published private var _showReviewSheet: Bool = false
    @Published private var _deleteTarget: ColorDeleteTarget? = nil
    
    @Published private var _newPassword: String = ""
    @Published private var _confirmPassword: String = ""
    @Published private var _showNewPassword: Bool = false
    @Published private var _showPassword: Bool = false
    @Published private var _showChangePasswordAlert: Bool = false
    @Published private var _showParConfiguration: Bool = false
    
    @Published var image: UIImage? = nil
    
    var editCourse: Bool {
        get { _editCourse }
        set { kotlin.setEditCourse(value: newValue) }
    }
    
    var showingPickerLogo: Bool {
        get { _showingPickerLogo }
        set { kotlin.setShowingPickerLogo(value: newValue) }
    }
    
    var showingPickerAd: Bool {
        get { _showingPickerAd }
        set { kotlin.setShowingPickerAd(value: newValue) }
    }
    
    var showReviewSheet: Bool {
        get { _showReviewSheet }
        set { kotlin.setShowReviewSheet(value: newValue) }
    }
    
    var deleteTarget: ColorDeleteTarget? {
        get { _deleteTarget }
        set { kotlin.setDeleteTarget(value: newValue) }
    }
    
    var newPassword: String {
        get { _newPassword }
        set { kotlin.setNewPassword(value: newValue) }
    }
    
    var confirmPassword: String {
        get { _confirmPassword }
        set { kotlin.setConfirmPassword(value: newValue) }
    }
    
    var showNewPassword: Bool {
        get { _showNewPassword }
        set { kotlin.setShowNewPassword(value: newValue) }
    }
    
    var showPassword: Bool {
        get { _showPassword }
        set { kotlin.setShowPassword(value: newValue) }
    }
    
    var showChangePasswordAlert: Bool {
        get { _showChangePasswordAlert }
        set { kotlin.setShowChangePasswordAlert(value: newValue) }
    }
    
    var showParConfiguration: Bool {
        get { _showParConfiguration }
        set { kotlin.setShowParConfiguration(value: newValue) }
    }
    
    var isValidPassword: Bool {
        kotlin.isValidPassword
    }

    init() {
        self.kotlin = KoinHelper.shared.getCourseSettingsViewModel()
        setupObservations()
    }

    private func setupObservations() {
        Task {
            for await val in kotlin.editCourse {
                self._editCourse = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.showingPickerLogo {
                self._showingPickerLogo = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.showingPickerAd {
                self._showingPickerAd = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.showReviewSheet {
                self._showReviewSheet = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.deleteTarget {
                self._deleteTarget = val
            }
        }
        
        Task {
            for await val in kotlin.newPassword {
                self._newPassword = val as String
            }
        }
        
        Task {
            for await val in kotlin.confirmPassword {
                self._confirmPassword = val as String
            }
        }
        
        Task {
            for await val in kotlin.showNewPassword {
                self._showNewPassword = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.showPassword {
                self._showPassword = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.showChangePasswordAlert {
                self._showChangePasswordAlert = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.showParConfiguration {
                self._showParConfiguration = val.boolValue
            }
        }
    }
    
    func uploadLogoImage(_ image: UIImage, course: Binding<Course?>) {
        kotlin.uploadLogoImageIOS(newImage: image, course: course.wrappedValue) { newCourse in
            course.wrappedValue = newCourse
        }
    }

    func uploadAdImage(_ image: UIImage, course: Binding<Course?>) {
        kotlin.uploadAdImageIOS(newImage: image, course: course.wrappedValue) { newCourse in
            course.wrappedValue = newCourse
        }
    }

    func changePassword(course: Binding<Course?>, userID: String?) {
        kotlin.changePassword(course: course.wrappedValue, userID: userID) { newCourse in
            course.wrappedValue = newCourse
        }
    }

    func deleteColor(target: ColorDeleteTarget, course: Binding<Course?>) {
        kotlin.deleteColor(target: target, course: course.wrappedValue) { newCourse in
            course.wrappedValue = newCourse
        }
    }
}

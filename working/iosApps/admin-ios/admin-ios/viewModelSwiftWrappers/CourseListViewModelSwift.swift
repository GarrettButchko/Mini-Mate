//
//  CourseListViewModelSwift.swift
//  admin-ios
//

import SwiftUI
import Combine
import shared_admin

@MainActor
class CourseListViewModelSwift: ObservableObject {
    let kotlin: CourseListViewModel
    
    @Published private var _password: String = ""
    @Published private var _message: String? = nil
    @Published private var _showAddCourseAlert: Bool = false
    @Published private var _loadingCourse: Bool = false
    @Published private var _userCourses: [Course] = []
    @Published private var _selectedCourse: Course? = nil
    @Published private var _timeRemaining: Double = 0.0
    @Published private var _failedAttempts: Int = 0
    @Published private var _addTarget: ColorAddTarget? = nil
    @Published private var _showColor: Bool = false
    
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()
    
    var password: String {
        get { _password }
        set { kotlin.setPassword(value: newValue) }
    }
    
    var message: String? {
        get { _message }
        set { kotlin.setMessage(value: newValue) }
    }
    
    var showAddCourseAlert: Bool {
        get { _showAddCourseAlert }
        set { kotlin.setShowAddCourseAlert(value: newValue) }
    }
    
    var loadingCourse: Bool {
        get { _loadingCourse }
        set { kotlin.setLoadingCourse(value: newValue) }
    }
    
    var userCourses: [Course] {
        get { _userCourses }
    }
    
    var selectedCourse: Course? {
        get { _selectedCourse }
        set { kotlin.setCourse(course: newValue) }
    }
    
    var timeRemaining: Double {
        get { _timeRemaining }
    }
    
    var failedAttempts: Int {
        get { _failedAttempts }
    }
    
    var addTarget: ColorAddTarget? {
        get { _addTarget }
        set { kotlin.setAddTarget(value: newValue) }
    }
    
    var showColor: Bool {
        get { _showColor }
        set { kotlin.setShowColor(value: newValue) }
    }
    
    var hasCourse: Bool {
        kotlin.hasCourse
    }
    
    var blockAddingCourse: Bool {
        kotlin.blockAddingCourse
    }
    
    var failedLimit: Int32 {
        kotlin.failedLimit
    }

    init() {
        self.kotlin = KoinHelper.shared.getCourseListViewModel()
        setupObservations()
    }

    private func setupObservations() {
        Task {
            for await val in kotlin.password {
                self._password = val as String
            }
        }
        
        Task {
            for await val in kotlin.message {
                self._message = val
            }
        }
        
        Task {
            for await val in kotlin.showAddCourseAlert {
                self._showAddCourseAlert = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.loadingCourse {
                self._loadingCourse = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.userCourses {
                withAnimation {
                    self._userCourses = val
                }
            }
        }
        
        Task {
            for await val in kotlin.selectedCourse {
                self._selectedCourse = val
            }
        }
        
        Task {
            for await val in kotlin.timeRemaining {
                self._timeRemaining = val.doubleValue
            }
        }
        
        Task {
            for await val in kotlin.failedAttempts {
                self._failedAttempts = val.intValue
            }
        }
        
        Task {
            for await val in kotlin.addTarget {
                self._addTarget = val
            }
        }
        
        Task {
            for await val in kotlin.showColor {
                self._showColor = val.boolValue
            }
        }
    }
    
    func binding<T>(
        keyPath: ReferenceWritableKeyPath<Course, T>,
        onSet: @escaping (T) -> Void = { _ in },
        debounce: Bool = false
    ) -> Binding<T>? {
        guard selectedCourse != nil else { return nil }
        return Binding(
            get: {
                guard let value = self.selectedCourse else {
                    fatalError("Course became nil unexpectedly")
                }
                return value[keyPath: keyPath]
            },
            set: { [weak self] newValue in
                self?.objectWillChange.send()
                self?.kotlin.updateCourseField(debounce: debounce) { course in
                    course[keyPath: keyPath] = newValue
                    return course
                }
                onSet(newValue)
            }
        )
    }
    
    func optionalBinding(
        keyPath: ReferenceWritableKeyPath<Course, String?>,
        deleteKey: String,
        debounce: Bool = true
    ) -> Binding<String> {
        Binding(
            get: {
                self.selectedCourse?[keyPath: keyPath] ?? ""
            },
            set: { [weak self] newValue in
                self?.objectWillChange.send()
                self?.kotlin.updateOptionalCourseField(
                    newValue: newValue,
                    deleteKey: deleteKey,
                    debounce: debounce
                ) { course, valueToSave in
                    course[keyPath: keyPath] = valueToSave
                    return course
                }
            }
        )
    }
    
    func socialPlatformBinding(
        index: Int,
        debounce: Bool = false
    ) -> Binding<SocialPlatform> {
        Binding(
            get: {
                guard let courseValue = self.selectedCourse,
                      index < courseValue.socialLinks.count else {
                    return .instagram
                }
                return courseValue.socialLinks[index].platform
            },
            set: { [weak self] newValue in
                self?.objectWillChange.send()
                self?.kotlin.updateSocialPlatform(index: Int32(index), newPlatform: newValue, debounce: debounce)
            }
        )
    }
    
    
    func limitedTextBinding(
        keyPath: ReferenceWritableKeyPath<Course, String?>,
        deleteKey: String,
        limit: Int,
        debounce: Bool = true
    ) -> Binding<String> {
        Binding(
            get: {
                self.selectedCourse?[keyPath: keyPath] ?? ""
            },
            set: { [weak self] newValue in
                self?.objectWillChange.send()
                self?.kotlin.updateLimitedCourseField(
                    newValue: newValue,
                    limit: Int32(limit),
                    deleteKey: deleteKey,
                    debounce: debounce
                ) { course, valueToSave in
                    course[keyPath: keyPath] = valueToSave
                    return course
                }
            }
        )
    }
    
    func customParBinding() -> Binding<Bool> {
        Binding(
            get: { self.selectedCourse?.customPar ?? false },
            set: { [weak self] newValue in
                self?.objectWillChange.send()
                self?.kotlin.updateCustomPar(customPar: newValue)
            }
        )
    }
    
    func numHolesBinding() -> Binding<Int> {
        Binding(
            get: { Int(self.selectedCourse?.numHoles ?? 18) },
            set: { [weak self] newValue in
                withAnimation {
                    self?.objectWillChange.send()
                    self?.kotlin.updateNumHoles(newHoles: Int32(newValue))
                }
            }
        )
    }
    
    func parBinding(index: Int) -> Binding<Int> {
        Binding(
            get: {
                guard let courseValue = self.selectedCourse,
                      index < courseValue.pars.count else { return 2 }
                return Int(truncating: courseValue.pars[index])
            },
            set: { [weak self] newValue in
                self?.objectWillChange.send()
                self?.kotlin.updatePar(index: Int32(index), newPar: Int32(newValue))
            }
        )
    }
}

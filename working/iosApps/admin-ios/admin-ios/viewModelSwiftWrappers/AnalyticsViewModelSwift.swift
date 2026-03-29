//
//  AnalyticsViewModelSwift.swift
//  admin-ios
//

import SwiftUI
import Combine
import shared_admin

@MainActor
class AnalyticsViewModelSwift: ObservableObject {
    let kotlin: AnalyticsViewModel
    
    @Published private var _range: AnalyticsRange = AnalyticsRange.Last30()
    @Published private var _selectedSection: AnalyticsSection = .growth
    @Published private var _pickedSection: String = "Day Range"
    @Published private var _allDailyDocs: [DailyDoc] = []
    @Published private var _healthReport: CourseHealthReport? = nil
    @Published private var _isLoadingHealth: Bool = false
    @Published private var _allEmails: [String: CourseEmail] = [:]
    @Published private var _loadingDocs: Bool = true
    @Published private var _loadingEmails: Bool = true
    @Published private var _currentCourse: Course? = nil
    
    var range: AnalyticsRange {
        get { _range }
        set { kotlin.setRange(newRange: newValue, course: currentCourse) }
    }
    
    var selectedSection: AnalyticsSection {
        get { _selectedSection }
        set { kotlin.setSelectedSection(section: newValue) }
    }
    
    var pickedSection: String {
        get { _pickedSection }
        set { kotlin.setPickedSection(section: newValue) }
    }
    
    var pickerSections: [String] {
        kotlin.pickerSections
    }
    
    var allDailyDocs: [DailyDoc] {
        get { _allDailyDocs }
    }
    
    var deltaDailyDocs: [DailyDoc] {
        kotlin.deltaDailyDocs
    }
    
    var rangeDailyDocs: [DailyDoc] {
        kotlin.rangeDailyDocs
    }
    
    var healthReport: CourseHealthReport? {
        get { _healthReport }
    }
    
    var isLoadingHealth: Bool {
        get { _isLoadingHealth }
    }
    
    var allEmails: [String: CourseEmail] {
        get { _allEmails }
    }
    
    var loadingDocs: Bool {
        get { _loadingDocs }
    }
    
    var loadingEmails: Bool {
        get { _loadingEmails }
    }
    
    var currentCourse: Course? {
        get { _currentCourse }
    }
    
    // Child ViewModels
    lazy var growthVM: GrowthViewModelSwift = { GrowthViewModelSwift(kotlin: kotlin.growthVM) }()
    lazy var operationsVM: OperationsViewModelSwift = { OperationsViewModelSwift(kotlin: kotlin.operationsVM) }()
    lazy var experienceVM: ExperienceViewModelSwift = { ExperienceViewModelSwift(kotlin: kotlin.experienceVM) }()
    lazy var retentionVM: RetentionViewModelSwift = { RetentionViewModelSwift(kotlin: kotlin.retentionVM) }()
    
    var analyticsObjects: [String: AnalyticsObject] {
        kotlin.analyticsObjects
    }

    init() {
        self.kotlin = KoinHelper.shared.getAnalyticsViewModel()
        setupObservations()
    }

    private func setupObservations() {
        Task {
            for await val in kotlin.range {
                self._range = val
            }
        }
        
        Task {
            for await val in kotlin.selectedSection {
                self._selectedSection = val
            }
        }
        
        Task {
            for await val in kotlin.pickedSection {
                self._pickedSection = val as String
            }
        }
        
        Task {
            for await val in kotlin.allDailyDocs {
                withAnimation {
                    self._allDailyDocs = val
                }
            }
        }
        
        Task {
            for await val in kotlin.healthReport {
                self._healthReport = val
            }
        }
        
        Task {
            for await val in kotlin.isLoadingHealth {
                self._isLoadingHealth = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.allEmails {
                self._allEmails = val
            }
        }
        
        Task {
            for await val in kotlin.loadingDocs {
                self._loadingDocs = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.loadingEmails {
                self._loadingEmails = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.currentCourse {
                self._currentCourse = val
            }
        }
    }

    func refreshAnalytics(course: Course?) {
        Task {
            do {
                try await kotlin.refreshAnalytics(course: course)
            } catch {
                print("Refresh error: \(error)")
            }
        }
    }

    func onAppearRetention(course: Course?) async throws {
        try await kotlin.onAppearRetention(course: course)
    }

    func onChange(old: AnalyticsRange, new: AnalyticsRange, course: Course?) {
        kotlin.onChange(old: old, new: new, course: course)
    }
}

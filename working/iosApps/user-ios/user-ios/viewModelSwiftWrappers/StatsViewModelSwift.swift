//
//  StatsViewModelSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/21/26.
//

import SwiftUI
import Combine
import shared_user

@MainActor
class StatsViewModelSwift: ObservableObject {
    let kotlin: StatsViewModel
    
    // 1. Observed properties from Kotlin Flows
    @Published private var _pickedSection: String = "Games"
    @Published private var _searchText: String = ""
    @Published private var _latest: Bool = true
    @Published private var _editOn: Bool = false
    @Published private var _editingGameID: String? = nil
    @Published private var _isSharePresented: Bool = false
    @Published private var _shareContent: String = ""
    @Published private var _isCooldown: Bool = false
    @Published private var _isCooldown2: Bool = false
    @Published private var _analyzer: UserStatsAnalyzer? = nil
    @Published private var _isRefreshing: Bool = false
    @Published private var _allGames: [Game] = []
    
    // 2. Public Computed Properties with Getters and Setters
    var pickedSection: String {
        get { _pickedSection }
        set { kotlin.setPickedSection(section: newValue) }
    }
    
    var searchText: String {
        get { _searchText }
        set { kotlin.setSearchText(text: newValue) }
    }
    
    var latest: Bool {
        get { _latest }
        set { kotlin.setLatest(value: newValue) }
    }
    
    var editOn: Bool {
        get { _editOn }
        set { kotlin.setEditOn(value: newValue) }
    }
    
    var editingGameID: String? {
        get { _editingGameID }
        set { kotlin.setEditingGameID(id: newValue) }
    }
    
    var isSharePresented: Bool {
        get { _isSharePresented }
        set { kotlin.setIsSharePresented(value: newValue) }
    }
    
    var shareContent: String {
        get { _shareContent }
    }
    
    var isCooldown: Bool {
        get { _isCooldown }
    }
    
    var isCooldown2: Bool {
        get { _isCooldown2 }
    }
    
    var analyzer: UserStatsAnalyzer? {
        get { _analyzer }
    }
    
    var isRefreshing: Bool {
        get { _isRefreshing }
    }
    
    var allGames: [Game] {
        get { _allGames }
    }
    
    let pickerSections: [String]

    init() {
        self.kotlin = KoinHelper.shared.getStatsViewModel()
        self.pickerSections = kotlin.pickerSections
        setupObservations()
    }

    private func setupObservations() {
        Task {
            for await val in kotlin.pickedSection {
                self._pickedSection = val
            }
        }
        
        Task {
            for await val in kotlin.searchText {
                self._searchText = val
            }
        }
        
        Task {
            for await val in kotlin.latest {
                self._latest = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.editOn {
                self._editOn = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.editingGameID {
                self._editingGameID = val
            }
        }
        
        Task {
            for await val in kotlin.isSharePresented {
                self._isSharePresented = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.shareContent {
                self._shareContent = val
            }
        }
        
        Task {
            for await val in kotlin.isCooldown {
                self._isCooldown = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.isCooldown2 {
                self._isCooldown2 = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.analyzer {
                self._analyzer = val
            }
        }
        
        Task {
            for await val in kotlin.isRefreshing {
                self._isRefreshing = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.allGames {
                self._allGames = val
            }
        }
    }
}

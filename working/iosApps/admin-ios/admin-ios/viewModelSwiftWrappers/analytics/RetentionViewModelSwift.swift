//
//  RetentionViewModelSwift.swift
//  admin-ios
//

import SwiftUI
import Combine
import shared_admin

class RetentionViewModelSwift: ObservableObject {
    let kotlin: RetentionViewModel

    init(kotlin: RetentionViewModel) {
        self.kotlin = kotlin
    }
    
    var allEmails: [String: CourseEmail] {
        get { kotlin.allEmails }
        set { kotlin.allEmails = newValue }
    }
    
    var cachedNewPlayers: [String: CourseEmail] {
        get { kotlin.cachedNewPlayers }
        set { kotlin.cachedNewPlayers = newValue }
    }
    
    var cachedMidTierPlayers: [String: CourseEmail] {
        get { kotlin.cachedMidTierPlayers }
        set { kotlin.cachedMidTierPlayers = newValue }
    }
    
    var cachedFrequentPlayers: [String: CourseEmail] {
        get { kotlin.cachedFrequentPlayers }
        set { kotlin.cachedFrequentPlayers = newValue }
    }
    
    var cachedAtRiskPlayers: [String: CourseEmail] {
        get { kotlin.cachedAtRiskPlayers }
        set { kotlin.cachedAtRiskPlayers = newValue }
    }
    
    var cachedAvgTimeToReturn: Int32 {
        get { kotlin.cachedAvgTimeToReturn }
        set { kotlin.cachedAvgTimeToReturn = newValue }
    }
    
    var cached30DayRetention: Double {
        get { kotlin.cached30DayRetention }
        set { kotlin.cached30DayRetention = newValue }
    }
    
    func getNewPlayers() -> [String: CourseEmail] {
        return kotlin.getNewPlayers()
    }
    
    func getMidTierPlayers() -> [String: CourseEmail] {
        return kotlin.getMidTierPlayers()
    }
    
    func getFrequentPlayers() -> [String: CourseEmail] {
        return kotlin.getFrequentPlayers()
    }
    
    func getAtRiskPlayers() -> [String: CourseEmail] {
        return kotlin.getAtRiskPlayers()
    }
    
    func isRecentlyActive(lastPlayedString: String?, days: Int32) -> Bool {
        return kotlin.isRecentlyActive(lastPlayedString: lastPlayedString, days: days)
    }
    
    func avgTimeToReturn() -> Int32 {
        return kotlin.avgTimeToReturn()
    }
    
    func getAvgTimeToReturn() -> DataPointObject {
        return kotlin.getAvgTimeToReturn()
    }
    
    func calculate30DayRetention() -> Double {
        return kotlin.calculate30DayRetention()
    }
    
    func get30DayRetention() -> DataPointObject {
        return kotlin.get30DayRetention()
    }
    
    func recomputePlayerTiers() {
        kotlin.recomputePlayerTiers()
    }
    
    func generateCSVContent(emails: [String]) -> String {
        return kotlin.generateCSVContent(emails: emails)
    }
}

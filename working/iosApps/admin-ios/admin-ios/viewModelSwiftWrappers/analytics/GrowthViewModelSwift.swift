//
//  GrowthViewModelSwift.swift
//  admin-ios
//

import SwiftUI
import Combine
import shared_admin


class GrowthViewModelSwift: ObservableObject {
    let kotlin: GrowthViewModel

    init(kotlin: GrowthViewModel) {
        self.kotlin = kotlin
    }
    
    var rangeDailyDocs: [DailyDoc] {
        get { kotlin.rangeDailyDocs }
        set { kotlin.rangeDailyDocs = newValue }
    }
    
    var deltaDailyDocs: [DailyDoc] {
        get { kotlin.deltaDailyDocs }
        set { kotlin.deltaDailyDocs = newValue }
    }
    
    var range: AnalyticsRange {
        get { kotlin.range }
        set { kotlin.range = newValue }
    }
    
    var growthChartTopic: ChartTopic {
        get { kotlin.growthChartTopic }
        set { kotlin.growthChartTopic = newValue }
    }
    
    func getActiveUsers(docs: [DailyDoc]) -> Int32 {
        return kotlin.getActiveUsers(docs: docs)
    }
    
    func getFirstTimeUsers(docs: [DailyDoc]) -> Int32 {
        return kotlin.getFirstTimeUsers(docs: docs)
    }
    
    func firstTimePercOfTotal() -> Double {
        return kotlin.firstTimePercOfTotal()
    }
    
    func getReturningUsers(docs: [DailyDoc]) -> Int32 {
        return kotlin.getReturningUsers(docs: docs)
    }
    
    func returningPercOfTotal() -> Double {
        return kotlin.returningPercOfTotal()
    }
    
    func avgPlayersPerGame(docs: [DailyDoc]) -> Double {
        return kotlin.avgPlayersPerGame(docs: docs)
    }
    
    func activeUsersPrime() -> DataPointObject {
        return kotlin.activeUsersPrime()
    }
    
    func firstTimePrime() -> DataPointObject {
        return kotlin.firstTimePrime()
    }
    
    func returningPrime() -> DataPointObject {
        return kotlin.returningPrime()
    }
    
    func avgPlayersPerGamePrime() -> DataPointObject {
        return kotlin.avgPlayersPerGamePrime()
    }
    
    func getDataForGrowthTrend() async throws -> [PlayerActivity] {
        return try await kotlin.getDataForGrowthTrend()
    }
}

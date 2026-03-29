//
//  ExperienceViewModelSwift.swift
//  admin-ios
//

import SwiftUI
import Combine
import shared_admin

class ExperienceViewModelSwift: ObservableObject {
    let kotlin: ExperienceViewModel

    init(kotlin: ExperienceViewModel) {
        self.kotlin = kotlin
    }
    
    var rangeDailyDocs: [DailyDoc] {
        get { kotlin.rangeDailyDocs }
        set { kotlin.rangeDailyDocs = newValue }
    }
    
    var currentCourse: Course? {
        get { kotlin.currentCourse }
        set { kotlin.currentCourse = newValue }
    }

    func getHardestHole() -> DataPointObject {
        return kotlin.getHardestHole()
    }

    func getEasiestHole() -> DataPointObject {
        return kotlin.getEasiestHole()
    }

    func getHoleCombined() -> [String: KotlinDouble] {
        return kotlin.getHoleCombined()
    }

    func getHoleDifficultyData() async throws -> [HoleDifficultyData] {
        return try await kotlin.getHoleDifficultyData()
    }

    func getHoleHeatmapForParData(course: Course) async throws -> [HoleHeatmapData] {
        return try await kotlin.getHoleHeatmapForParData(course: course)
    }

    func getAvgRelativeToPar() -> DataPointObject {
        return kotlin.getAvgRelativeToPar()
    }

    func getMostBeatenPar() -> DataPointObject {
        return kotlin.getMostBeatenPar()
    }

    func getUnderParPercentage() -> DataPointObject {
        return kotlin.getUnderParPercentage()
    }

    func getOverParPercentage() -> DataPointObject {
        return kotlin.getOverParPercentage()
    }

    func getHoleInOneCount() -> DataPointObject {
        return kotlin.getHoleInOneCount()
    }
}

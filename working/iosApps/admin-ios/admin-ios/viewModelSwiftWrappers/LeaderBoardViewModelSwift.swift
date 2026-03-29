//
//  LeaderBoardViewModelSwift.swift
//  admin-ios
//

import SwiftUI
import Combine
import shared_admin

@MainActor
class LeaderBoardViewModelSwift: ObservableObject {
    let kotlin: LeaderBoardViewModel
    
    @Published private var _allTimeLeaderboard: [LeaderboardEntry] = []
    
    var allTimeLeaderboard: [LeaderboardEntry] {
        get { _allTimeLeaderboard }
        set { kotlin.setAllTimeLeaderboard(leaderboard: newValue) }
    }
    
    init() {
        self.kotlin = KoinHelper.shared.getLeaderBoardViewModel()
        setupObservations()
    }

    private func setupObservations() {
        Task {
            for await val in kotlin.allTimeLeaderboard {
                withAnimation {
                    self._allTimeLeaderboard = val
                }
            }
        }
    }
}

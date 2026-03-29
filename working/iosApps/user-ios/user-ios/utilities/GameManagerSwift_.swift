//
//  GameManagerSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/21/26.
//

import SwiftUI
import Combine


import shared_user


@MainActor
class GameManagerSwift: ObservableObject {
    let kotlin: GameManager
    
    @Published private var _userGames: [Game] = []
    @Published private var _isRefreshing: Bool = false
    
    var userGames: [Game] {
        _userGames
    }
    
    var isRefreshing: Bool {
        _isRefreshing
    }
    
    init() {
        // Accessing the GameManager instance from Koin
        self.kotlin = KoinHelper.shared.getGameManager()
        setupObservations()
    }
    
    private func setupObservations() {
        // Observe the list of user games
        Task {
            for await games in kotlin.userGames {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                    self._userGames = games
                }
            }
        }
        
        // Observe the refreshing state
        Task {
            for await refreshing in kotlin.isRefreshing {
                self._isRefreshing = refreshing.boolValue
            }
        }
    }
}

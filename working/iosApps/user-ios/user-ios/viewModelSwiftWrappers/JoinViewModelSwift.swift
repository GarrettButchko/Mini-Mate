//
//  JoinViewModelSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/21/26.
//

import SwiftUI
import Combine
import shared_user

@MainActor
class JoinViewModelSwift: ObservableObject {
    let kotlin: JoinViewModel
    
    // 1. Observed properties from Kotlin Flows
    @Published private var _gameCode: String = ""
    @Published private var _inGame: Bool = false
    @Published private var _showExitAlert: Bool = false
    @Published private var _message: String = ""
    
    // 2. Public Computed Properties with Getters and Setters
    var gameCode: String {
        get { _gameCode }
        set { kotlin.setGameCode(code: newValue) }
    }
    
    var inGame: Bool {
        get { _inGame }
    }
    
    var showExitAlert: Bool {
        get { _showExitAlert }
        set { kotlin.setShowExitAlert(show: newValue) }
    }
    
    var message: String {
        get { _message }
    }

    init() {
        self.kotlin = KoinHelper.shared.getJoinViewModel()
        setupObservations()
    }

    private func setupObservations() {
        Task {
            for await val in kotlin.gameCode {
                self._gameCode = val
            }
        }
        
        Task {
            for await val in kotlin.inGame {
                self._inGame = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.showExitAlert {
                self._showExitAlert = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.message {
                self._message = val
            }
        }
    }
}

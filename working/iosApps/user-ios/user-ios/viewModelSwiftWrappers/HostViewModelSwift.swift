//
//  HostViewModelSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/21/26.
//

import SwiftUI
import Combine
import shared_user
import MapKit

@MainActor
class HostViewModelSwift: ObservableObject {
    let kotlin: HostViewModel
    
    // 1. Observed properties from Kotlin Flows
    @Published private var _timeRemaining: Double = 0
    @Published private var _playerToDelete: String? = nil
    @Published private var _showTextAndButtons: Bool = false
    @Published private var _isRotating: Bool = false
    @Published private var _showLocationButton: Bool = false
    @Published private var _qrCodeImage: UIImage? = nil
    @Published private var _showQRCode: Bool = false
    @Published private var _showAddLocalPlayer: Bool = false
    @Published private var _showDeleteAlert: Bool = false
    
    // 2. Public Computed Properties with Getters and Setters
    var timeRemaining: Double {
        get { _timeRemaining }
    }
    
    var playerToDelete: String? {
        get { _playerToDelete }
        set { kotlin.setPlayerToDelete(id: newValue) }
    }
    
    var showTextAndButtons: Bool {
        get { _showTextAndButtons }
        set { kotlin.setShowTextAndButtons(value: newValue) }
    }
    
    var isRotating: Bool {
        get { _isRotating }
        set { kotlin.setIsRotating(value: newValue) }
    }
    
    var showLocationButton: Bool {
        get { _showLocationButton }
        set { kotlin.setShowLocationButton(value: newValue) }
    }
    
    var qrCodeImage: UIImage? {
        get { _qrCodeImage }
    }
    
    var showQRCode: Bool {
        get { _showQRCode }
        set { kotlin.setShowQRCode(value: newValue) }
    }
    
    var showAddLocalPlayer: Bool {
        get { _showAddLocalPlayer }
        set { kotlin.setShowAddLocalPlayer(value: newValue) }
    }
    
    var showDeleteAlert: Bool {
        get { _showDeleteAlert }
        set { kotlin.setShowDeleteAlert(value: newValue) }
    }

    init() {
        self.kotlin = KoinHelper.shared.getHostViewModel()
        setupObservations()
    }

    private func setupObservations() {
        Task {
            for await val in kotlin.timeRemaining {
                self._timeRemaining = val.doubleValue
            }
        }
        
        Task {
            for await val in kotlin.playerToDelete {
                self._playerToDelete = val
            }
        }
        
        Task {
            for await val in kotlin.showTextAndButtons {
                self._showTextAndButtons = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.isRotating {
                self._isRotating = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.showLocationButton {
                self._showLocationButton = val.boolValue
            }
        }
        
        Task {
            for await _ in kotlin.qrCodeImage {
                // Whenever qrCodeImage changes, update our local UIImage using the Kotlin helper
                self._qrCodeImage = kotlin.qrCodeUIImage()
            }
        }
        
        Task {
            for await val in kotlin.showQRCode {
                self._showQRCode = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.showAddLocalPlayer {
                self._showAddLocalPlayer = val.boolValue
            }
        }
        
        Task {
            for await val in kotlin.showDeleteAlert {
                self._showDeleteAlert = val.boolValue
            }
        }
    }
    
    // MARK: - Timer Methods
    
    func tick(showHost: Binding<Bool>) {
        kotlin.tick(onTimeout: { bool in
            showHost.wrappedValue = bool.boolValue
        })
    }
    
    func startTimer(showHost: Binding<Bool>) {
        kotlin.startTimer(onTimeout: { bool in
            showHost.wrappedValue = bool.boolValue
        })
    }

    func startGame(showHost: Binding<Bool>, isGuest: Bool = false) {
        kotlin.startGame(onHostHidden: {
            showHost.wrappedValue = false
        }, isGuest: isGuest)
    }
}

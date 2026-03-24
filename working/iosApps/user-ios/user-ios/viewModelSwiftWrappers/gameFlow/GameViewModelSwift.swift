//
//  GameViewModelSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/21/26.
//

import SwiftUI
import Combine
import shared_user

@MainActor
class GameViewModelSwift: ObservableObject {
    let kotlin: GameViewModel
    
    // 1. Observed properties from Kotlin Flows
    @Published private var _game: Game
    @Published private var _course: Course?
    
    // 2. Public Computed Properties with Getters and Setters
    var game: Game {
        get { _game }
        set { kotlin.setGame(newGame: newValue, listen: true) }
    }

    /// Alias for compatibility with existing views
    var gameValue: Game {
        get { _game }
    }
    
    var course: Course? {
        get { _course }
        set { kotlin.setCourse(newCourse: newValue) }
    }

    var isOnline: Bool {
        kotlin.onlineGame
    }
    
    /// Two-way binding: DatePicker(..., selection: vm.binding(for: \ .date))
    func binding<T>(for keyPath: ReferenceWritableKeyPath<Game, T>) -> Binding<T> {
        Binding(
            get: { self.game[keyPath: keyPath] },
            set: { newValue in
                self.game[keyPath: keyPath] = newValue
                self.kotlin.pushUpdate()
            }
        )
    }

    func bindingForGame() -> Binding<Game> {
        Binding(
            get: { self.game },
            set: { self.game = $0 }
        )
    }
    
    init() {
        self.kotlin = KoinHelper.shared.getGameViewModel()
        
        let placeholderGame = Game(id: "", hostUserId: "", date: Firebase_firestoreTimestamp.companion.now(), completed: false, numberOfHoles: 18, started: false, dismissed: false, live: false, lastUpdated: Firebase_firestoreTimestamp.companion.now(), courseID: nil, locationName: "", startTime: Firebase_firestoreTimestamp.companion.now(), endTime: Firebase_firestoreTimestamp.companion.now(), players: [])
        
        self._game = placeholderGame
        
        setupObservations()
    }

    private func setupObservations() {
        // Observe game updates
        Task {
            for await gameValue in kotlin.game {
                self._game = gameValue
            }
        }
        
        // Observe course updates
        Task {
            for await courseValue in kotlin.course {
                self._course = courseValue
            }
        }
    }
    
    func startGame(showHost: Binding<Bool>) {
        kotlin.startGame { onHostHidden in
            showHost.wrappedValue = onHostHidden.boolValue
        }
    }
    
    func searchNearby(isLoading: Binding<Bool>) async {
        do {
            try await kotlin.searchNearby { bool in
                isLoading.wrappedValue = bool.boolValue
            } isLoading2: { bool in
                isLoading.wrappedValue = bool.boolValue
            }
        } catch {
            print("Error searching nearby: \(error)")
        }
    }
    
    func retry(isRotating: Binding<Bool>, isLoading: Binding<Bool>) async {
        do {
            // We call the Kotlin suspend function
            try await kotlin.retry(
                // 1. firstRotate: (Boolean) -> Unit
                firstRotate: { bool in
                    isRotating.wrappedValue = bool.boolValue
                },
                // 2. secondRotate: (Boolean) -> Unit
                secondRotate: { bool in
                    isRotating.wrappedValue = bool.boolValue
                },
                // 3. isLoading1: (Boolean) -> Unit
                isLoading1: { bool in
                    isLoading.wrappedValue = bool.boolValue
                },
                // 4. isLoading2: (Boolean) -> Unit
                isLoading2: { bool in
                    isLoading.wrappedValue = bool.boolValue
                }
            )
        } catch {
            print("Retry failed: \(error)")
        }
    }
}

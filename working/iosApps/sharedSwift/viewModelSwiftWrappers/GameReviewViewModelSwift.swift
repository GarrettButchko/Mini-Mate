//
//  GameReviewViewModelSwift.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/21/26.
//
import Combine
#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif

@MainActor
class GameReviewViewModelSwift: ObservableObject {
    let kotlinVM: GameReviewViewModel
    
    @Published private(set) var course: Course? = nil
    
    init(game: Game) {
        // Injected via the Parent helper
        self.kotlinVM = KoinHelperParent.shared.getGameReviewViewModel(game: game)
        setupObservations()
    }

    private func setupObservations() {
        Task {
            for await courseData in kotlinVM.course {
                self.course = courseData
            }
        }
    }
}

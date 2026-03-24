import SwiftUI

import shared_user

struct AddToLeaderBoardButton: View {
    
    @State var course: Course?
    @State var added: Bool = false
    // Track if the network request is currently in flight
    @State private var isSubmitting: Bool = false
    
    let player: Player
    let courseLeaderBoardRepo = CourseLeaderboardRepository()
    
    var body: some View {
        // Only show if all conditions are met
        if let course = course,
           !(ProfanityFilter.shared.containsBlockedWord(text: player.name)) &&
            !player.incomplete &&
            course.tier >= 2 &&
            player.email != nil &&
            course.customPar &&
            course.leaderBoardActive{
            
            Group {
                if added {
                    // --- Confirmation View ---
                    HStack(spacing: 4) {
                        Image(systemName: "checkmark.circle.fill")
                        Text("Added")
                    }
                    .font(.caption.bold())
                    .foregroundStyle(.green)
                    .frame(width: 120, height: 20)
                    .transition(.scale.combined(with: .opacity))
                } else {
                    // --- Action Button ---
                    Button {
                        Task{
                            await submit()
                        }
                    } label: {
                        ZStack {
                            RoundedRectangle(cornerRadius: 25)
                                .foregroundStyle(isSubmitting ? .gray : .blue)
                            
                            HStack {
                                if isSubmitting {
                                    ProgressView()
                                        .controlSize(.mini)
                                        .tint(.white)
                                } else {
                                    Image(systemName: "plus")
                                    Text("Leaderboard")
                                }
                            }
                            .font(.caption)
                            .foregroundStyle(.white)
                        }
                        .frame(width: 120, height: 20)
                    }
                    .disabled(isSubmitting) // Prevent multiple taps
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }
            }
            .animation(.spring(), value: added)
            .animation(.spring(), value: isSubmitting)
        }
    }
    
    private func submit() async {
        guard let courseID = course?.id else { return }
        
        isSubmitting = true
            do {
                // Kotlin 'suspend' becomes Swift 'async throws'
                let result = try await courseLeaderBoardRepo.submitScore(courseID: courseID, player: player)
                
                // Since Kotlin returns a Result<Boolean>, you need to check
                // the success/failure of that specific Kotlin object.
                if (result as AnyObject).description.contains("Success") {
                    self.added = true
                    print("Score submitted successfully!")
                } else {
                    print("Submission failed")
                }
                
            } catch {
                // This catches 'system' level errors (e.g., the bridge failing)
                print("Network or System Error: \(error.localizedDescription)")
            }
        self.isSubmitting = false
    }
}


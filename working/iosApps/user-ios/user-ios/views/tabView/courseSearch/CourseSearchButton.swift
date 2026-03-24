//
//  CourseSearchButton.swift
//  MiniMate
//
//  Created by Garrett Butchko on 3/13/26.
//
import SwiftUI
import shared_user

// MARK: - Search Button
struct CourseSearchButton: View {
    @EnvironmentObject var courseViewModel: CourseViewModelSwift
    
    var body: some View {
        Button {
            courseViewModel.kotlin.searchNearby()
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: 25)
                    .fill(Color.blue)
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(.white)
                    Text("Search for Nearby Courses")
                        .font(.headline)
                        .foregroundColor(.white)
                }
            }
        }
        .frame(height: 50)
        .transition(.move(edge: .bottom).combined(with: .opacity).combined(with: .blurReplace()))
        .cardShadow()
    }
}

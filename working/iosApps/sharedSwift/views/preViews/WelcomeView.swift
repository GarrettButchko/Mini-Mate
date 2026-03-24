//
//  WelcomeView.swift
//  MiniMate
//
//  Created by Garrett Butchko on 2/6/25.
//

import SwiftUI

#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif

extension Color {
    static let managerBlue = Color(red: 0.12, green: 0.25, blue: 0.45)
    static let managerGreen = Color(red: 0.18, green: 0.45, blue: 0.38)
}

struct WelcomeView: View {
    @StateObject private var viewModel: WelcomeViewModelSwift
    let gradientColors: [Color]

    init(welcomeText: String = "Welcome to MiniMate", gradientColors: [Color] = [.blue, .green]) {
        self.gradientColors = gradientColors
        _viewModel = StateObject(
            wrappedValue: WelcomeViewModelSwift(welcomeText: welcomeText)
        )
    }

    var body: some View {
        ZStack {
            Rectangle()
                .foregroundStyle(Gradient(colors: gradientColors))
                .ignoresSafeArea()

            VStack(spacing: 30) {
                Text(viewModel.displayedText)
                    .font(.largeTitle.bold())
                    .multilineTextAlignment(.center)
                    .padding()
                    .foregroundStyle(.white)

                Image("Logo")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 100, height: 100)
                    .colorScheme(.light)

                if viewModel.showLoading {
                    VStack(spacing: 16) {
                        Text("Trying to reconnect...")
                            .foregroundStyle(.white)

                        ProgressView()
                            .scaleEffect(1.5)
                    }
                }
            }
        }
        .onAppear {
            viewModel.kotlinVM.onAppear()
        }
        .onDisappear {
            viewModel.kotlinVM.onDisappear()
        }
    }
}

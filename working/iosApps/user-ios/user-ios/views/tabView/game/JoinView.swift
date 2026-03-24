// JoinView.swift
// MiniMate
//
// Refactored to use SwiftData models and AuthViewModel

import SwiftUI
import shared_user

struct JoinView: View {
    @Environment(\.modelContext) private var context
    @StateObject private var viewModel = JoinViewModelSwift()
    @EnvironmentObject var viewManager: ViewManagerSwift
    @EnvironmentObject var gameModel: GameViewModelSwift
    
    @Binding var showHost: Bool
    @State private var showScanner = false
    @State private var viewContentHeight: CGFloat = 0
    @State private var contentMargins: CGFloat = 70
    
    // Platform-agnostic business logic isolated for KMP
    private let logic = JoinViewBusinessLogic()
    
    var body: some View {
        ZStack{
            mainContent
                .contentMargins(.top, contentMargins)
                .contentMargins(.bottom, contentMargins)
            
            headerOverlay
            bottomButtons
        }
        .onChange(of: showHost) { oldValue, newValue in
            viewModel.kotlin.hostDidDismiss(showHost: showHost)
        }
        .onChange(of: gameModel.game.started) { _, newValue in
            viewModel.kotlin.gameDidStart(started: newValue) {
                viewManager.kotlinVM.navigateToScoreCard(isGuest: false)
            }
        }
        .onChange(of: gameModel.game.dismissed) { _, newValue in
            viewModel.kotlin.gameDidDismiss(dismissed: newValue)
        }
        .background { backgroundLayer }
    }
    
    // MARK: - Extracted Layers
    
    private var headerOverlay: some View {
        VStack {
            HStack {
                Spacer()
                Text("Join Game")
                    .font(.title)
                    .fontWeight(.bold)
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                    .padding(.top, 20)
                Spacer()
            }
            .padding(.bottom)
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [
                        (viewModel.inGame ? Color.sub.opacity(1) : Color.bg.opacity(1)),
                        Color.clear
                    ]),
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea(edges: .top)
            )
            Spacer()
        }
    }
    
    private var bottomButtons: some View {
        VStack {
            Spacer()
            Group {
                if viewModel.inGame {
                    exitButton
                } else {
                    joinButton
                }
            }
            .padding(.top)
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [
                        (viewModel.inGame ? Color.sub.opacity(1) : Color.bg.opacity(1)),
                        Color.clear
                    ]),
                    startPoint: .bottom,
                    endPoint: .top
                )
                .ignoresSafeArea(edges: .bottom)
            )
        }
    }
    
    private var exitButton: some View {
        Button {
            viewModel.showExitAlert = true
        } label: {
            Text("Exit Game")
                .font(.headline)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 14)
                .background(
                    RoundedRectangle(cornerRadius: 25)
                        .fill(Color.red)
                        .padding(.horizontal)
                )
        }
        .foregroundColor(.red)
        .alert("Exit Game?", isPresented: $viewModel.showExitAlert) {
            Button("Leave", role: .destructive) {
                viewModel.kotlin.leaveGame()
            }
            Button("Cancel", role: .cancel) {}
        }
    }
    
    private var joinButton: some View {
        Button {
            viewModel.kotlin.joinGame()
        } label: {
            HStack(alignment: .center){
                Image(systemName: "person.2.badge.plus.fill")
                Text("Join Game")
            }
            .font(.headline)
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
            .background(
                RoundedRectangle(cornerRadius: 25)
                    .fill(Color.blue)
                    .padding(.horizontal)
            )
            .opacity(logic.getJoinButtonOpacity(gameCode: viewModel.gameCode))
            .safeAreaPadding(.bottom, 10)
        }
        .disabled(logic.isJoinButtonDisabled(gameCode: viewModel.gameCode))
    }
    
    private var backgroundLayer: some View {
        GeometryReader { proxy in
            Color.bg.ignoresSafeArea()
                .task(id: proxy.size) {
                    withAnimation {
                        viewContentHeight = proxy.size.height - contentMargins - contentMargins
                    }
                }
        }
    }
    
    // MARK: - Content Sections
    
    private var mainContent: some View {
        Group {
            if !viewModel.inGame {
                joinGameCard
            } else {
                activeGameLobby
            }
        }
    }
    
    // MARK: - Join Game View
    @ViewBuilder
    private var joinGameCard: some View {
        ScrollView{
            VStack(spacing: 24) {
                // Header
                VStack(spacing: 8) {
                    Text("Enter Game Code")
                        .font(.system(.title2, design: .rounded))
                        .fontWeight(.bold)
                    
                    Text("Ask the host for the 6-digit code")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                
                // Input & Scanner
                VStack(spacing: 20) {
                    gameCodeTextField
                    actionDivider
                    scanButton
                }
            }
            .padding(.vertical, 32)
            .padding(.horizontal, 20)
            .background {
                RoundedRectangle(cornerRadius: 25, style: .continuous)
                    .fill(.sub)
                    .cardShadow()
            }
            
            if !viewModel.message.isEmpty {
                Text(viewModel.message)
                    .font(.footnote)
                    .foregroundStyle(.red.opacity(0.8))
                    .transition(.blurReplace)
                    .multilineTextAlignment(.center)
                    .padding(.vertical, 8)
                    .padding(.horizontal, 16)
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(.red.opacity(0.2))
                    )
            }
        }
        .frame(minHeight: viewContentHeight, alignment: .center)
    }
    
    // MARK: - Sub-Components
    private var gameCodeTextField: some View {
        TextField("000000", text: $viewModel.gameCode)
            .font(.system(size: 34, weight: .bold, design: .monospaced))
            .multilineTextAlignment(.center)
            .keyboardType(.asciiCapable)
            .textInputAutocapitalization(.characters)
            .disableAutocorrection(true)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(.subTwo)
            )
            .frame(width: 240)
            .onChange(of: viewModel.gameCode) { _, newValue in
                viewModel.gameCode = logic.formatEnteredCode(code: newValue)
            }
    }

    private var actionDivider: some View {
        HStack {
            Rectangle().fill(.separator).frame(height: 1)
            Text("OR").font(.caption2).fontWeight(.black).foregroundStyle(.tertiary)
            Rectangle().fill(.separator).frame(height: 1)
        }
        .padding(.horizontal, 40)
    }

    private var scanButton: some View {
        Button {
            showScanner = true
        } label: {
            Label("Scan QR Code", systemImage: "qrcode.viewfinder")
                .fontWeight(.medium)
                .foregroundStyle(.primary)
                .padding(.vertical, 10)
                .padding(.horizontal, 16)
                .background(Capsule().fill(.ultraThinMaterial))
        }
        .sheet(isPresented: $showScanner) {
            camView
                .presentationDragIndicator(.visible)
        }
    }

    // MARK: - Active Game Lobby
    @ViewBuilder
    private var activeGameLobby: some View {
        Form {
            playersSection
            
            Section(header: Text("Game Info")) {
                infoRow(title: "Code:", value: gameModel.game.id)
                infoRow(title: "Date:", value: gameModel.game.date.formatted())
                infoRow(title: "Holes:", value: "\(gameModel.game.numberOfHoles)")
                infoRow(title: "Location:", value: logic.getLocationName(name: gameModel.game.locationName))
            }
        }
    }

    private func infoRow(title: String, value: String) -> some View {
        HStack {
            Text(title)
            Spacer()
            Text(value)
                .foregroundStyle(.secondary)
        }
    }
    
    private var camView: some View {
        ZStack {
            QRScannerView { scannedCode in
                let formattedCode = logic.formatScannedCode(code: scannedCode)
                
                DispatchQueue.main.async {
                    viewModel.gameCode = formattedCode
                    withAnimation {
                        showScanner = false
                    }
                }
            }
            .ignoresSafeArea()
            
            Color.black.opacity(0.6)
                .mask {
                    Rectangle()
                        .overlay(
                            RoundedRectangle(cornerRadius: 30)
                                .frame(width: 260, height: 260)
                                .blendMode(.destinationOut)
                        )
                }
                .overlay{
                    ScannerCornerShape()
                        .stroke(.white, style: StrokeStyle(lineWidth: 8, lineCap: .round, lineJoin: .round))
                        .frame(width: 260, height: 260)
                }
                .ignoresSafeArea()
            
            VStack {
                VStack(spacing: 8) {
                    Text("Scan QR Code")
                        .font(.system(.title2, design: .rounded))
                        .fontWeight(.bold)
                    
                    Text("Center the code inside the frame")
                        .font(.subheadline)
                }
                .padding(.top, 60)
                .foregroundStyle(.white)
                
                Spacer()
                
                Button {
                    showScanner = false
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.system(size: 44))
                        .foregroundStyle(.white.opacity(0.8))
                }
                .padding(.bottom, 40)
            }
        }
    }
    
    private var playersSection: some View {
        Section(header: Text(logic.getPlayersHeaderText(playerCount: Int32(gameModel.game.players.count)))) {
            ScrollView(.horizontal) {
                HStack {
                    ForEach(gameModel.game.players) { player in
                        PlayerIconView(player: player, isRemovable: false) {}
                    }
                    VStack {
                        ProgressView()
                            .scaleEffect(1.5)
                            .frame(width: 40, height: 40)
                        Text("Waiting...").font(.caption)
                    }.padding(.horizontal)
                }
            }
            .frame(height: 75)
        }
    }
}

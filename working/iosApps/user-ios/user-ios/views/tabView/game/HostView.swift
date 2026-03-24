// HostView.swift
// MiniMate
//
// Refactored to use new SwiftData models and AuthViewModel

import SwiftUI
import MapKit
import shared_user

struct HostView: View {
    @Environment(\.scenePhase) private var scenePhase
    @EnvironmentObject var gameModel: GameViewModelSwift
    @EnvironmentObject var locationHandler: LocationHandlerSwift
    @EnvironmentObject var authModel: AuthViewModelSwift
    @EnvironmentObject var viewManager: ViewManagerSwift
    @EnvironmentObject var VM: HostViewModelSwift
    let localRepo: LocalGameRepository = KoinHelperParent.shared.getLocalGameRepo()
    
    @Binding var showHost: Bool
    
    var isGuest: Bool
    
    // Platform-agnostic business logic isolated for KMP
    private let logic = HostViewBusinessLogic()
    
    init(
        showHost: Binding<Bool>,
        isGuest: Bool = false
    ) {
        self._showHost = showHost
        self.isGuest = isGuest
    }
    
    var body: some View {
        mainContent
    }
    
    private var mainContent: some View {
        ZStack{
            
            Form {
                playersSection
                gameInfoSection
            }
            .contentMargins(.top, 100)
            .contentMargins(.bottom, 70)
            .onTapGesture {
                VM.kotlin.resetTimer()
            }
            
            VStack{
                HStack(alignment: .center, spacing: 10){
                    
                    if isGuest {
                        Button {
                            logic.handleGuestBackAction(
                                dismissGame: { gameModel.kotlin.dismissGame() },
                                navigateToSignIn: { viewManager.kotlinVM.navigateToSignIn() }
                            )
                        } label: {
                            Image(systemName: "chevron.left")
                                .resizable()
                                .scaledToFit()
                                .foregroundStyle(.blue)
                                .frame(width: 20, height: 20)
                        }
                    } else {
                        Color.clear
                            .frame(width: 20, height: 20)
                    }
                    
                    Spacer()
                    
                    Text(logic.getHeaderTitle(isOnline: gameModel.kotlin.onlineGame))
                        .font(.title)
                        .fontWeight(.bold)
                        .padding(.horizontal)
                    
                    Spacer()
                    
                    Color.clear
                        .frame(width: 20, height: 20)
                }
                .padding(.horizontal, 8)
                .padding(.top, isGuest ? 12 : 28)
                .background(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color.sub.opacity(1),
                            Color.clear
                        ]),
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .ignoresSafeArea(edges: .top)
                )
                    
                Spacer()
                
                startGameSection
                    .padding(.top)
                    .padding(.bottom, isGuest ? 40 : 0)
                    .background(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.sub.opacity(1),
                                Color.clear
                            ]),
                            startPoint: .bottom,
                            endPoint: .top
                        )
                        .ignoresSafeArea(edges: .bottom)
                    )
                    
            }
        }
        .sheet(isPresented: $VM.showAddLocalPlayer) {
            AddLocalPlayerView(showColor: $VM.showAddLocalPlayer)
        }
        .onAppear {
            VM.kotlin.resetTimer()
            VM.kotlin.setUp()
        }
        .onDisappear {
            VM.kotlin.resetTimer()
            
            let shouldDismiss = logic.shouldDismissGameOnDisappear(
                isStarted: gameModel.game.started,
                isDismissed: gameModel.game.dismissed,
                showHost: showHost
            )
            
            if shouldDismiss {
                gameModel.kotlin.dismissGame()
            }
        }
        .contentShape(Rectangle())
        .alert("Delete Player?", isPresented: $VM.showDeleteAlert) {
            Button("Delete", role: .destructive) {
                logic.handleDeletePlayer(playerId: VM.playerToDelete) { id in
                    gameModel.kotlin.removePlayer(userId: id)
                }
            }
            Button("Cancel", role: .cancel) {}
        }
        .onReceive(VM.timer) { _ in
            VM.tick(showHost: $showHost)
        }
        .environmentObject(VM)
    }
    
    // MARK: - Sections
    private var gameInfoSection: some View {
        let course = gameModel.kotlin.course
        return Group {
            Section {
                if gameModel.kotlin.onlineGame {
                    VStack{
                        HStack {
                            Text("Game Code:")
                            Spacer()
                            Text(gameModel.game.id)
                            Image(systemName: "qrcode")
                                .font(.system(size: 20, weight: .medium))
                        }
                        
                        Image(uiImage: VM.qrCodeImage ?? UIImage(systemName: "xmark.circle") ?? UIImage())
                            .interpolation(.none)
                            .resizable()
                            .scaledToFit()
                            .frame(height: 200)
                            .padding()
                            .background(
                                RoundedRectangle(cornerRadius: 16)
                                    .fill(.ultraThinMaterial)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(Color.primary.opacity(0.1))
                            )
                    }
                    
                    
                    HStack {
                        Text("Expires in:")
                        Spacer()
                        Text(VM.kotlin.timeString())
                            .monospacedDigit()
                    }
                }
                
                if locationHandler.hasLocationAccess{
                    LocationButtons()
                }
                
                if let course = course.value {
                    if course.customPar {
                        UserInfoRow(label: "Holes", value: String(course.numHoles))
                    } else {
                        HStack {
                            Text("Holes:")
                            NumberPickerView(
                                selectedNumber: Binding(
                                    get: { Int(gameModel.binding(for: \.numberOfHoles).wrappedValue) },
                                    set: { gameModel.binding(for: \.numberOfHoles).wrappedValue = Int32($0) }
                                ),
                                minNumber: 9, maxNumber: 21
                            )
                        }
                    }
                } else {
                    // No course → show picker
                    HStack {
                        Text("Holes:")
                        NumberPickerView(
                            selectedNumber: Binding(
                                get: { Int(gameModel.binding(for: \.numberOfHoles).wrappedValue) },
                                set: { gameModel.binding(for: \.numberOfHoles).wrappedValue = Int32($0) }
                            ),
                            minNumber: 9, maxNumber: 21
                        )
                    }
                }
            } header: {
                Text("Game Info")
            }
        }
    }
    
    private var playersSection: some View {
        Group{
            Section {
                ScrollView(.horizontal) {
                    HStack {
                        ForEach(gameModel.game.players) { player in
                            PlayerIconView(player: player, isRemovable: player.userId.count == 6) {
                                VM.playerToDelete = player.userId
                                VM.kotlin.resetTimer()
                                VM.showDeleteAlert = true
                            }
                        }
                        Button(action: {
                            VM.showAddLocalPlayer = true
                            VM.kotlin.resetTimer()
                        }) {
                            VStack {
                                ZStack {
                                    Circle()
                                        .fill(.ultraThinMaterial)
                                        .frame(width: 40, height: 40)
                                    Image(systemName: "plus")
                                }
                                Text("Add Player").font(.caption)
                            }
                            .padding(.horizontal)
                        }
                        if gameModel.kotlin.onlineGame {
                            VStack {
                                ProgressView()
                                    .scaleEffect(1.5)
                                    .frame(width: 40, height: 40)
                                Text("Searching...").font(.caption)
                            }.padding(.horizontal)
                        }
                    }
                }
                .simultaneousGesture(
                    DragGesture().onChanged { _ in
                        VM.kotlin.resetTimer()
                    }
                )
                .frame(height: 75)
            }  header: {
                Text(logic.getPlayersHeaderText(playerCount: Int32(gameModel.game.players.count)))
            }
        }
    }
    
    private var startGameSection: some View {
        Button {
            logic.handleStartGame(
                isGuest: isGuest,
                deleteGuestGame: {
                    Task{
                        do {
                            let _ = try await localRepo.deleteGuestGame()
                        } catch {
                            print("Error deleting guest game: \(error)")
                        }
                    }
                },
                performStart: {
                    VM.startGame(showHost: $showHost, isGuest: isGuest)
                }
            )
        } label: {
            HStack{
                Image(systemName: "play.fill")
                Text("Start Game")
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
        }
        
    }
}

struct LocationButtons: View {
    
    @EnvironmentObject var VM: HostViewModelSwift
    @EnvironmentObject var gameModel: GameViewModelSwift
    @EnvironmentObject var locationHandler: LocationHandlerSwift
    
    var body: some View {
        let item = gameModel.kotlin.course.value?.name
        let isConnected = NetworkChecker.companion.shared.isConnected
    
        Group{
            if VM.showLocationButton && isConnected {
                HStack{
                    VStack{
                        HStack{
                            Text("Location:")
                            Spacer()
                        }
                        
                        if let item = item {
                            HStack{
                                Text(item)
                                    .foregroundStyle(.secondary)
                                    .truncationMode(.tail)
                                    .transition(.move(edge: .top).combined(with: .opacity))
                                Spacer()
                            }
                        } else {
                            HStack{
                                Text("No Location")
                                    .foregroundStyle(.secondary)
                                    .transition(.move(edge: .top).combined(with: .opacity))
                                Spacer()
                            }
                        }
                    }
                    
                    Spacer()
                    
                    
                    if item == nil {
                        searchNearbyButton
                    } else {
                        retryButton
                        exitButton
                    }
                }
                
            } else if let item = item, !VM.showLocationButton && isConnected {
                HStack{
                    Text("Location:")
                    Spacer()
                    Text(item)
                }
            }
        }
        
    }
    
    var noButtons: some View{
        Group{
            if let item = gameModel.kotlin.course.value?.name {
                HStack{
                    Spacer()
                    Text(item)
                        .foregroundStyle(.secondary)
                        .truncationMode(.tail)
                        .transition(.move(edge: .top).combined(with: .opacity))
                }
            } else {
                HStack{
                    Spacer()
                    Text("No location found")
                        .foregroundStyle(.secondary)
                        .transition(.move(edge: .top).combined(with: .opacity))
                }
            }
        }
    }
    
    var searchNearbyButton: some View {
        Button {
            VM.kotlin.searchNearby()
        } label: {
            HStack(spacing: 6) {
                Image(systemName: "magnifyingglass")
                Text("Search Nearby")
            }
            .frame(width: 180, height: 50)
            .background(Color.blue)
            .foregroundColor(.white)
            .clipShape(RoundedRectangle(cornerRadius: 20))
        }
        .buttonStyle(.plain)
        .transition(.move(edge: .trailing).combined(with: .opacity))
    }
    
    var retryButton: some View {
        Button(action: {
            withAnimation(){
                VM.kotlin.retry()
            }
        }) {
            Image(systemName: "arrow.trianglehead.2.clockwise")
                .rotationEffect(.degrees(VM.isRotating ? 360 : 0))
                .font(.title2)
                .foregroundColor(.white)
                .frame(width: 50, height: 50)
                .background(Color.blue)
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .transition(.move(edge: .trailing).combined(with: .opacity))
    }
    
    var exitButton: some View {
        Button(action: {
            withAnimation {
                VM.kotlin.exit()
            }
        }) {
            Image(systemName: "xmark")
                .font(.title2)
                .foregroundColor(.white)
                .frame(width: 50, height: 50)
                .background(Color.red)
                .clipShape(Circle())
        }
        .buttonStyle(.plain)
        .transition(.move(edge: .trailing).combined(with: .opacity))
    }
}

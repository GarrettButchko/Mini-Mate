//
//  StatsView.swift
//  MiniMate
//
//  Created by Garrett Butchko on 2/6/25.
//

import SwiftUI
import Charts
import MarqueeText
import _SwiftData_SwiftUI
import shared_user

struct StatsView: View {
    
    @Environment(\.modelContext) private var context
    @StateObject private var statModel = StatsViewModelSwift()
    @EnvironmentObject var viewManager: ViewManagerSwift
    @EnvironmentObject var authModel: AuthViewModelSwift
    @EnvironmentObject var gameManager: GameManagerSwift
    
    var usersGames: [Game] { statModel.allGames }
    
    var games: [Game] {
        let filteredGames: [Game]
        if statModel.latest {
            filteredGames = usersGames.sorted { $0.date > $1.date }
        } else {
            filteredGames = usersGames.sorted { $0.date < $1.date }
        }
        return filteredGames
    }
    
    @State private var isDismissed = false
    @State var isRotating: Bool = false
    @State var gameReview: Game? = nil
    
    @State private var titleHeight: CGFloat = 0
    
    var body: some View {
        if (authModel.userModel != nil) {
            VStack(spacing: 12) {
                HStack {
                    ZStack {
                        if statModel.pickedSection == "Games" {
                            Text("Game Stats")
                                .font(.title).fontWeight(.bold)
                                .transition(.opacity.combined(with: .scale))
                        } else {
                            Text("Overview")
                                .font(.title).fontWeight(.bold)
                                .transition(.opacity.combined(with: .scale))
                        }
                    }
                    .background {
                        GeometryReader { proxy in
                            Color.clear
                                .onAppear {
                                    titleHeight = proxy.size.height
                                }
                                .onChange(of: proxy.size.height) { _, newValue in
                                    titleHeight = newValue
                                }
                        }
                    }
                    .animation(.easeInOut(duration: 0.35), value: statModel.pickedSection)
                    
                    Spacer()
                    
                    if statModel.pickedSection == "Games" {
                        Button {
                            statModel.kotlin.toggleSortWithCooldown()
                        } label: {
                            ZStack{
                                Circle()
                                    .ifAvailableGlassEffect()
                                    .frame(width: titleHeight, height: titleHeight)
                                
                                if statModel.latest{
                                    Image(systemName: "arrow.up")
                                        .transition(.scale)
                                        .frame(width: 50, height: 50)
                                } else {
                                    Image(systemName: "arrow.down")
                                        .transition(.scale)
                                        .frame(width: 50, height: 50)
                                }
                            }
                        }
                        .transition(.opacity.combined(with: .scale))
                    }
                }
                .frame(height: 50)
                .animation(.easeInOut(duration: 0.35), value: statModel.pickedSection)
                
                Picker("Section", selection: $statModel.pickedSection) {
                    ForEach(statModel.pickerSections, id: \.self) {
                        Text($0)
                    }
                }
                .pickerStyle(.segmented)
                
                
                ZStack {
                    if statModel.pickedSection == "Games" {
                        gamesSection
                            .transition(.asymmetric(
                                insertion: .move(edge: .leading).combined(with: .opacity),
                                removal: .move(edge: .leading).combined(with: .opacity)
                            ))
                            .contentMargins(.vertical, 12)
                    } else {
                        
                        if gameManager.kotlin.hasGames() == true {
                            overViewSection
                                .transition(.asymmetric(
                                    insertion: .move(edge: .trailing).combined(with: .opacity),
                                    removal: .move(edge: .trailing).combined(with: .opacity)
                                ))
                                .onAppear {
                                    statModel.editOn = false
                                }
                                .contentMargins(.vertical, 12)
                        } else {
                            VStack(spacing: 8) {
                                Spacer()
                                Image("logoOpp")
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 48, height: 48)
                                    .opacity(0.8)
                                
                                Text("No Games Yet")
                                    .font(.headline)
                                
                                Text("Hit the course to see your stats appear.")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                                    .multilineTextAlignment(.center)
                                Spacer()
                            }
                            .padding(.vertical, 24)
                            .onAppear {
                                statModel.editOn = false
                            }
                        }
                    }
                }
                .animation(.easeInOut(duration: 0.3), value: statModel.pickedSection)
            }
            .background(.bg)
            .safeAreaPadding([.horizontal])
            .sheet(isPresented: $statModel.isSharePresented) {
                ActivityView(activityItems: [statModel.shareContent])
            }
            .onAppear{
                statModel.kotlin.onAppear()
            }
        }
    }
    
    private var gamesSection: some View {
        
            ScrollView {
                VStack(spacing: 16){
                    if NetworkChecker.companion.shared.isConnected && !authModel.userModel!.isPro {
                        VStack{
                            BannerAdView(adUnitID: "ca-app-pub-8261962597301587/6344452429") // Replace with real one later
                                .frame(height: 50)
                                .padding()
                        }
                        .background(
                            RoundedRectangle(cornerRadius: 25)
                                .fill(.sub)
                                .cardShadow()
                        )
                    }
                    
                    if !authModel.userModel!.isPro && authModel.userModel!.gameIDs.count >= 2 {
                        Text("You’ve reached the free limit. Upgrade to Pro to store more than 2 games.")
                            .padding()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(
                                RoundedRectangle(cornerRadius: 25)
                                    .fill(.sub)
                                    .cardShadow()
                            )
                    }
                    
                    
                    if statModel.isRefreshing {
                        ProgressView()
                    } else if !games.isEmpty {
                        
                        
                            ForEach(games, id: \.id) { game in // Explicitly use .id to help transitions
                                GameRow(
                                    editOn: $statModel.editOn,
                                    editingGameID: $statModel.editingGameID,
                                    gameReview: $gameReview,
                                    game: game,
                                    presentShareSheet: statModel.kotlin.presentShareSheet
                                )
                                .transition(.asymmetric(insertion: .opacity, removal: .opacity.combined(with: .move(edge: .leading))))
                                .sheet(item: $gameReview) {
                                    gameReview = nil
                                } content: { game in
                                    GameReviewView(game: game, showBackToStatsButton: true)
                                        .presentationDragIndicator(.visible)
                                }
                                .cardShadow()
                            }
                        
                        LogoDefault(topPadding: 0)
                    } else if !authModel.userModel!.gameIDs.isEmpty && games.isEmpty {
                        // Game IDs exist but SwiftData hasn't loaded them yet - show loading state
                        VStack(spacing: 16) {
                            ProgressView()
                            
                            Text("Loading Games...")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        .padding(.vertical, 32)
                    } else {
                        VStack(spacing: 8) {
                            Image("logoOpp")
                                .resizable()
                                .scaledToFit()
                                .frame(width: 48, height: 48)
                                .opacity(0.8)

                            Text("No Games Yet!")
                                .font(.headline)

                            Text("Play a game to get started.")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .padding(.vertical, 24)
                    }
                }
            }
        
        .environmentObject(statModel)
    }
    
    private var overViewSection: some View {
        let spacing : CGFloat = 10
        
        return ScrollView {
            VStack(spacing: 16){
                SectionStatsView(title: "Basic Stats", spacing: spacing) {
                    HStack(spacing: spacing){
                        StatCard(title: "Players Faced", value: "\(gameManager.kotlin.totalPlayersFaced())", infoText: "Number of players other than yourself you played with.")
                        StatCard(title: "Holes Played", value: "\(gameManager.kotlin.totalHolesPlayed())", infoText: "Total holes played (including unfinished holes).")
                    }
                    
                    StatCard(title: "Games Played", value: "\(gameManager.kotlin.totalGamesPlayed())", infoText: "Total games played.")
                    
                    HStack(spacing: spacing){
                        StatCard(title: "Strokes/Game", value: String(format: "%.1f", gameManager.kotlin.averageStrokesPerGame()), infoText: "Average strokes per game.")
                        StatCard(title: "Strokes/Hole", value: String(format: "%.1f", gameManager.kotlin.averageStrokesPerHole()), infoText: "Average strokes per hole.")
                    }
                }
                
                
                if NetworkChecker.companion.shared.isConnected && !authModel.userModel!.isPro {
                    VStack{
                        BannerAdView(adUnitID: "ca-app-pub-8261962597301587/6344452429") // Replace with real one later
                            .frame(height: 50)
                            .padding()
                    }
                    .background(
                        RoundedRectangle(cornerRadius: 25)
                            .fill(.sub)
                            .cardShadow()
                    )
                }
                
                SectionStatsView(title: "Average 18-Hole Game", spacing: spacing){
                    BarChartView(data: gameManager.kotlin.averageHoles18(), title: "Average Strokes")
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(.subTwo)
                        )
                }
                
                SectionStatsView(title: "Misc Stats", spacing: spacing) {
                    HStack(spacing: spacing){
                        StatCard(title: "Best Game", value: "\(gameManager.kotlin.bestGameStrokes() ?? 0)", color: .green, infoText: "Lowest total strokes in a game.")
                        StatCard(title: "Worst Game", value: "\(gameManager.kotlin.worstGameStrokes() ?? 0)", color: .red, infoText: "Highest total strokes in a game.")
                    }
                    StatCard(title: "Holes-in-One", value: "\(gameManager.kotlin.holeInOneCount())", infoText: "Number of holes with one stroke.")
                }
                
                SectionStatsView(title: "Average 9-Hole Game", spacing: spacing){
                    BarChartView(data: gameManager.kotlin.averageHoles9(), title: "Average Strokes")
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .fill(.subTwo)
                        )
                }
            }
        }
    }
}




struct GameGridView: View {
    @Binding var editOn: Bool
    @EnvironmentObject var authModel: AuthViewModelSwift
    var game: Game

    var sortedPlayers: [Player] {
        game.players.sorted(by: { $0.totalStrokes < $1.totalStrokes })
    }

    // FIXED: Bracket mismatch in GameGridView.body
    var body: some View {
        VStack(alignment: .leading, spacing: 16) { // Adds vertical spacing
            // Game Info & Players Row
            HStack(alignment: .top, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {

                    if let gameLocName = game.locationName{
                        MarqueeText(
                            text: gameLocName,
                            font: UIFont.preferredFont(forTextStyle: .title3),
                            leftFade: 16,
                            rightFade: 16,
                            startDelay: 2 // recommend 1–2 seconds for a subtle Apple-like pause
                        )
                        .foregroundStyle(.mainOpp)
                        .font(.title3).fontWeight(.bold)

                        Text("\(game.startTime.formatted()) - \(game.numberOfHoles) Holes")
                            .font(.caption).foregroundColor(.secondary)
                    } else {
                        MarqueeText(
                            text: game.date.formatted(),
                            font: UIFont.preferredFont(forTextStyle: .title3),
                            leftFade: 16,
                            rightFade: 16,
                            startDelay: 2 // recommend 1–2 seconds for a subtle Apple-like pause
                        )
                        .foregroundStyle(.mainOpp)
                        .font(.title3).fontWeight(.bold)

                        Text("\(game.numberOfHoles) Holes")
                            .font(.caption).foregroundColor(.secondary)
                    }
                }


                if game.players.count > 1 {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) { // Player icon spacing
                            ForEach(game.players) { player in
                                if game.players.count != 0{
                                    if player.id != game.players[0].id {
                                        Divider()
                                            .frame(height: 50)
                                    }
                                }

                                if sortedPlayers[0] == player {
                                    PhotoIconView(photoURL: player.photoURL, name: player.name + "🥇", ballColor: player.ballColor, imageSize: 20, background: Color.yellow)
                                } else {
                                    PhotoIconView(photoURL: player.photoURL, name: player.name, ballColor: player.ballColor, imageSize: 20, background: .ultraThinMaterial)
                                }

                            }
                        }
                    }
                    .frame(height: 50)
                } else {
                    if sortedPlayers.count != 0 {
                        PhotoIconView(photoURL: sortedPlayers[0].photoURL, name: sortedPlayers[0].name, ballColor: sortedPlayers[0].ballColor, imageSize: 20, background: .ultraThinMaterial)
                    }
                }

            }

            // Bar Chart
            BarChartView(data: averageStrokes(), title: "Average Strokes")
                .background(
                    RoundedRectangle(cornerRadius: 12)
                        .fill(.subTwo)
                )
        }
        .padding()
        .background(.sub)
        .clipShape(RoundedRectangle(cornerRadius: 25))
        .frame(height: 250)
    }




    /// Returns one Hole per hole-number, whose `strokes` is the integer average
    /// across all players for that hole.
    func averageStrokes() -> [Hole] {
        let holeCount   = game.numberOfHoles
        let playerCount = game.players.count
        guard playerCount > 0 else { return [] }

        // 1) Sum strokes per hole index (0-based)
        var sums = [Int](repeating: 0, count: Int(holeCount))
        for player in game.players {
            for hole in player.holes {
                let idx = hole.number - 1
                sums[Int(idx)] += Int(hole.strokes)
            }
        }

        // 2) Build averaged Hole objects
        return sums.enumerated().map { (idx, total) in
            let avg = total / playerCount
            return Hole(id: generateUUID(), number: Int32(idx) + 1, strokes: Int32(avg))
        }
    }
}

struct GameRow: View {
    @EnvironmentObject var authModel: AuthViewModelSwift
    @EnvironmentObject var viewManager: ViewManagerSwift
    @EnvironmentObject var statModel: StatsViewModelSwift

    @Binding var editOn: Bool
    @Binding var editingGameID: String?

    @Binding var gameReview: Game?

    var game: Game
    var presentShareSheet: (String) -> Void

    var localGameRepo: LocalGameRepository = KoinHelperParent.shared.getLocalGameRepo()
    var remoteGameRepo: RemoteGameRepository = KoinHelperParent.shared.getRemoteGameRepo()
    var remoteUserRepo: RemoteUserRepository = KoinHelperParent.shared.getRemoteUserRepo()

    var body: some View {
        GeometryReader { proxy in
            HStack{
                GameGridView(editOn: $editOn, game: game)
                    .frame(width: proxy.size.width)
                    .transition(.opacity.combined(with: .blurReplace))
                    .swipeMod(editingID: $editingGameID, id: game.id,
                              buttonPressFunction: {
                        gameReview = game
                    },
                              buttonOne: NetworkChecker.companion.shared.isConnected ? ButtonSkim(color: Color.blue, systemImage: "square.and.arrow.up", string: makeShareableSummary(for: game)) : nil
                    ) {
                        statModel.kotlin.deleteGame(gameID: game.id)
                    }
                    
            }
        }
        .frame(height: 250)
    }

    /// Build a plain-text summary (you could also return a URL to a generated PDF/image)
    func makeShareableSummary(for game: Game) -> String {
        var lines = ["MiniMate Scorecard",
                     "Date: \(game.date.formatted())",
                     ""]

        for player in game.players {
            var holeLine = ""

            for hole in player.holes {
                holeLine += "|\(hole.strokes)"
            }

            lines.append("\(player.name): \(player.totalStrokes) strokes (\(player.totalStrokes))")
            lines.append("Holes " + holeLine)

        }
        lines.append("")
        lines.append("Download MiniMate: https://apps.apple.com/app/id6745438125")
        return lines.joined(separator: "\n")
    }
}

struct DarkenOnPressButtonStyle: ButtonStyle {
    var darkenOpacity: Double = 0.22
    var animation: Animation = .easeInOut(duration: 0.12)

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .overlay(
                Color.black
                    .opacity(configuration.isPressed ? darkenOpacity : 0)
                    .allowsHitTesting(false)
            )
            .animation(animation, value: configuration.isPressed)
    }
}

struct ActivityView: UIViewControllerRepresentable {
    var activityItems: [Any]
    var applicationActivities: [UIActivity]? = nil
    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems,
                                 applicationActivities: applicationActivities)
    }
    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

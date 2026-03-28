import SwiftUI
import StoreKit
import MapKit
import shared_user

struct MainView: View {
    @EnvironmentObject var locationHandler: LocationHandlerSwift
    
    // Platform-agnostic business logic isolated for KMP
    private let logic = MainViewBusinessLogic()
    
    var disablePlaying: Bool {
        logic.isPlayDisabled(
            isPro: KotlinBoolean(nonretainedObject: authModel.userModel?.isPro),
            gameCount: Int32(authModel.userModel?.gameIDs.count ?? 0)
        )
    }
    
    @Environment(\.colorScheme) private var colorScheme
    
    @EnvironmentObject var viewManager: ViewManagerSwift
    @EnvironmentObject var authModel: AuthViewModelSwift
    @EnvironmentObject var gameModel: GameViewModelSwift
    @EnvironmentObject var gameManager: GameManagerSwift
    
    @State private var nameIsPresented = false
    @State private var isSheetPresented = false
    @State var isOnlineMode = false
    @State var showHost = false
    @State var showJoin = false
    @State var showFirstStage: Bool = false
    @State var alreadyShown: Bool = false
    @State var editOn: Bool = false
    @State var showDonation: Bool = false
    @State var showInfo: Bool = false
    @State var isRotating: Bool = false
    @State var gameReview: Game? = nil
    
    private var localGameRepo: LocalGameRepository = KoinHelperParent.shared.getLocalGameRepo()
    
    @State private var showLastGameStats = false
    @State private var buttonsViewHeight: CGFloat = 0
    @State private var isLoading = false
    
    private var userGameIDs: [String] {
        authModel.userModel?.gameIDs ?? []
    }
    
    var body: some View {
        let course = gameModel.course
        
        VStack{
            topBar
                .padding(.horizontal)
            
            ZStack {
                scrollContent(course: course)
                actionButtonsSection(course: course)
                    
            }
            .padding(.top)
            .contentMargins(.horizontal, 16)
            .ignoresSafeArea(.keyboard)
        }
        .onAppear {
            gameManager.kotlin.onAppear()
            if NetworkChecker.companion.shared.isConnected {
                Task {
                   try? await gameModel.kotlin.setUp()
                }
            }
            
            // Initial check to see if games are already loaded
            if !gameManager.userGames.isEmpty {
                showLastGameStats = true
            }
        }
        .onChange(of: authModel.userModel) { _, _ in
            gameManager.kotlin.onAppear()
        }
        .onChange(of: gameManager.userGames.count) { _, newValue in
            // When games are loaded or change, ensure the stats section is visible if applicable
            if newValue > 0 && !showLastGameStats {
                withAnimation(.spring()) {
                    showLastGameStats = true
                }
            } else if newValue == 0 {
                showLastGameStats = false
            }
        }
        .background{
            Rectangle()
                .fill(.bg)
                .ignoresSafeArea()
        }
    }

    
    @State private var titleHeight: CGFloat = 0
    
    // MARK: - Main Sections
    private var topBar: some View {
        let course = gameModel.course
        return VStack(spacing: 16) {
            
            HStack {
                HStack{
                    VStack(alignment: .leading, spacing: 2){
                        Text("Welcome back,")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        
                        Text(logic.getGreetingName(name: authModel.userModel?.name))
                            .font(.title2)
                            .fontWeight(.semibold)
                    }
                    .background {
                        GeometryReader { proxy in
                            Color.clear
                                .task(id: proxy.size) {
                                    titleHeight = proxy.size.height // Capture the size and monitor changes
                                }
                        }
                    }
                    
                    if let courseLogo = course?.logo {
                        Divider()
                        
                        AsyncImage(url: URL(string: courseLogo)) { image in
                            image
                                .resizable()
                                .scaledToFit()
                        } placeholder: {
                            ProgressView()
                        }
                        .id(URL(string: courseLogo))
                    }
                }
                .frame(maxHeight: titleHeight)
                
                Spacer()
                
                profilePhotoButton
            }
            
            TitleView(colors: course?.courseColors)
                .frame(height: 150)
        }
    }
    
    private var profilePhotoButton: some View {
        Button(action: {
            isSheetPresented = true
        }) {
            if let photoURL = authModel.firebaseUser?.photoURL {
                AsyncImage(url: URL(string: photoURL)) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    Image("logoOpp")
                        .resizable()
                        .scaledToFill()
                }
                .frame(width: 40, height: 40)
                .clipShape(Circle())
                .id(photoURL)
            } else {
                Image("logoOpp")
                    .resizable()
                    .scaledToFill()
                    .frame(width: 40, height: 40)
            }
        }
        .sheet(isPresented: $isSheetPresented) {
            ProfileView(isSheetPresent: $isSheetPresented)
        }
    }
    
    private func scrollContent(course: Course?) -> some View {
        Group {
            if authModel.userModel != nil {
                ScrollView {
                    VStack(spacing: 16) {
                        if NetworkChecker.companion.shared.isConnected {
                            locationButtons(course: course)
                                .cardShadow()
                        }
                        
                        proStopper
                            .cardShadow()
                        
                        ad
                            .cardShadow()
                        
                        lastGameStats
                        
                    }
                    .padding(.top, 16)
                }
                .contentMargins(.top, buttonsViewHeight) // subtract 25 to allow some overlap for aesthetic
                .scrollIndicators(.hidden)
            }
        }
    }
    
    private func actionButtonsSection(course: Course?) -> some View {
        VStack {
            VStack {
                headerControls
                gameModeButtons
            }
            .padding()
            .background(){
                GeometryReader { proxy in
                    RoundedRectangle(cornerRadius: 25)
                        .ifAvailableGlassEffect(strokeWidth: 0, opacity: 0.5, makeColor: course?.scoreCardColor) // Create a transparent view matching the parent's size
                        .task(id: proxy.size) {
                            buttonsViewHeight = proxy.size.height // Capture the size and monitor changes
                        }
                }
            }
            .clipped()
            .padding(.horizontal)
            .cardShadow()
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.bg.opacity(1),
                        Color.clear
                    ]),
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea(edges: .top)
            )
            
            Spacer()
            
            proBuyButton
        }
    }
    
    private var headerControls: some View {
        HStack(alignment: .top){
            ZStack {
                if isOnlineMode {
                    Button(action: {
                        withAnimation(.easeInOut) {
                            isOnlineMode = false
                        }
                    }) {
                        ZStack {
                            Circle()
                                .fill(.primary)
                                .frame(width: 30, height: 30)
                            Image(systemName: "chevron.left")
                                .foregroundStyle(.white)
                                .frame(width: 20, height: 20)
                        }
                    }
                } else {
                    Circle()
                        .fill(Color.clear)
                        .frame(width: 30, height: 30)
                }
            }
            
            Spacer()
            
            ZStack {
                Text(logic.getHeaderTitle(isOnlineMode: isOnlineMode))
                    .id(isOnlineMode)
                    .font(.title)
                    .fontWeight(.bold)
                    .lineLimit(1)
                    .transition(.move(edge: .top).combined(with: .blurReplace).combined(with: .scale).combined(with: .opacity))
            }
            .frame(maxWidth: .infinity)
            .animation(.spring(response: 0.5, dampingFraction: 0.7), value: isOnlineMode)
            
            Spacer()
            
            Button {
                showInfo = true
            } label: {
                Image(systemName: "info.circle")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 20, height: 20)
                    .foregroundStyle(.blue)
            }
            .alert("Info", isPresented: $showInfo) {
                Button("OK") {}
            } message: {
                Text(logic.getInfoMessage(isOnlineMode: isOnlineMode))
            }
        }
    }
    
    private var gameModeButtons: some View {
        ZStack {
            if isOnlineMode {
                onlineGameButtons
            } else {
                offlineGameButtons
            }
        }
        .animation(.spring(response: 0.45, dampingFraction: 0.75), value: isOnlineMode)
        
    }
    
    private var onlineGameButtons: some View {
        HStack(spacing: 16) {
            gameModeButton(title: "Host", icon: "antenna.radiowaves.left.and.right", color: .purple) {
                gameModel.kotlin.createGame(online: true, guestData: nil)
                withAnimation(.easeInOut) {
                    showHost = true
                }
            }
            .sheet(isPresented: $showHost) {
                HostView(showHost: $showHost)
                    .presentationDetents([.large])
                    .presentationDragIndicator(.visible)
            }
            
            gameModeButton(title: "Join", icon: "person.2.fill", color: .orange) {
                gameModel.kotlin.resetGame()
                withAnimation(.easeInOut) {
                    showJoin = true
                }
            }
            .sheet(isPresented: $showJoin) {
                JoinView(showJoin: $showJoin)
                    .presentationDetents([.large])
                    .presentationDragIndicator(.visible)
            }
        }
        .transition(.move(edge: .trailing).combined(with: .opacity).combined(with: .blurReplace))
        .clipped()
    }
    
    private var offlineGameButtons: some View {
        HStack(spacing: 16) {
            gameModeButton(title: "Quick", icon: "person.fill", color: .blue) {
                if !disablePlaying {
                    gameModel.kotlin.createGame(online: false, guestData: nil)
                    withAnimation(.easeInOut) {
                        isOnlineMode = false
                        showHost = true
                    }
                } else {
                    showDonation = true
                }
            }
            .sheet(isPresented: $showHost) {
                HostView(showHost: $showHost)
                    .presentationDetents([.large])
                    .presentationDragIndicator(.visible)
                
            }
            
            if !NetworkChecker.companion.shared.isConnected {
                disconnectedButton
            } else {
                gameModeButton(title: "Connect", icon: "globe", color: .green) {
                    if !disablePlaying {
                        withAnimation(.easeInOut) {
                            isOnlineMode = true
                        }
                    } else {
                        showDonation = true
                    }
                }
            }
        }
        .transition(.move(edge: .leading).combined(with: .opacity).combined(with: .blurReplace))
    }
    
    private var disconnectedButton: some View {
        HStack {
            Image(systemName: "globe")
            Text("Connect")
                .fontWeight(.semibold)
        }
        .padding(10)
        .frame(maxWidth: .infinity, minHeight: 50)
        .background(RoundedRectangle(cornerRadius: 15).fill().foregroundStyle(.green))
        .foregroundColor(.white)
        .opacity(0.4)
    }
    
    private var proBuyButton: some View {
        Group {
            if let userModel = authModel.userModel, !userModel.isPro && NetworkChecker.companion.shared.isConnected {
                HStack {
                    Spacer()
                    Button {
                        if !showFirstStage {
                            withAnimation {
                                showFirstStage = true
                            }
                            
                            DispatchQueue.main.asyncAfter(deadline: .now() + logic.proPromotionDisplayDuration) {
                                if showFirstStage {
                                    withAnimation {
                                        showFirstStage = false
                                    }
                                }
                            }
                        } else {
                            showDonation = true
                        }
                    } label: {
                        HStack {
                            if showFirstStage {
                                Text("Tap to buy Pro!")
                                    .transition(.move(edge: .trailing).combined(with: .opacity))
                                    .foregroundStyle(.white)
                            }
                            Text("✨")
                        }
                        .padding()
                        .frame(height: 50)
                        .background {
                            RoundedRectangle(cornerRadius: 25)
                                .ifAvailableGlassEffect(strokeWidth: 0, opacity: 0.7, makeColor: .purple)
                                .cardShadow()
                        }
                        .cardShadow(radius: 10, y: 0)
                    }
                    .sheet(isPresented: $showDonation) {
                        ProView(showSheet: $showDonation)
                    }
                    .padding()
                }
            }
        }
    }
    
    func locationButtons(course: Course?) -> some View {
        Group {
            if NetworkChecker.companion.shared.isConnected {
                HStack{
                    if locationHandler.hasLocationAccess {
                        VStack{
                            HStack{
                                Text("Location:")
                                Spacer()
                            }
                            
                            if let item = course?.name {
                                HStack{
                                    Text(item)
                                        .foregroundStyle(.secondary)
                                        .truncationMode(.tail)
                                        .transition(.move(edge: .top).combined(with: .opacity).combined(with: .blurReplace))
                                    Spacer()
                                }
                            } else {
                                HStack{
                                    Text("No Location")
                                        .foregroundStyle(.secondary)
                                        .transition(.move(edge: .top).combined(with: .opacity).combined(with: .blurReplace))
                                    Spacer()
                                }
                            }
                        }
                        .animation(.spring, value: course?.name)
                        
                        Spacer()
                        
                        
                        if course == nil {
                            Button {
                                Task{
                                    await gameModel.searchNearby(isLoading: $isLoading)
                                }
                            } label: {
                                HStack(spacing: 8) {
                                    if isLoading {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                            .scaleEffect(0.8)
                                        Text("Searching...")
                                            .fontWeight(.medium)
                                    } else {
                                        Image(systemName: "magnifyingglass")
                                            .font(.body.weight(.semibold))
                                        Text("Search Nearby")
                                            .fontWeight(.semibold)
                                    }
                                }
                                .frame(width: 160, height: 50)
                                .background(
                                    RoundedRectangle(cornerRadius: 15)
                                        .fill(Color.blue)
                                )
                                .foregroundStyle(.white)
                                .transition(.move(edge: .trailing).combined(with: .opacity))
                                .animation(.spring, value: isLoading)
                            }
                            .buttonStyle(.plain)
                            .transition(.move(edge: .trailing).combined(with: .opacity).combined(with: .blurReplace))
                            
                        } else {
                            // Retry Button
                            Button(action: {
                                Task{
                                    await gameModel.retry(isRotating: $isRotating, isLoading: $isLoading)
                                }
                            }) {
                                Image(systemName: "arrow.trianglehead.2.clockwise")
                                    .rotationEffect(.degrees(isRotating ? 360 : 0))
                                    .font(.title2)
                                    .foregroundColor(.white)
                                    .frame(width: 50, height: 50)
                                    .background(Color.blue)
                                    .clipShape(Circle())
                            }
                            .buttonStyle(.plain)
                            .transition(.move(edge: .trailing).combined(with: .opacity).combined(with: .blurReplace))
                            .animation(.spring, value: isRotating)
                            
                            
                            // Exit Button
                            Button(action: {
                                withAnimation {
                                    gameModel.kotlin.exit()
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
                            .transition(.move(edge: .trailing).combined(with: .opacity).combined(with: .blurReplace))
                        }
                    } else {
                        VStack(spacing: 16) {
                            Image(systemName: "location.slash.circle.fill")
                                .font(.system(size: 50))
                                .foregroundStyle(.secondary)
                                .symbolEffect(.bounce, value: locationHandler.hasLocationAccess)

                            VStack(spacing: 8) {
                                Text("Location Access Required")
                                    .font(.headline)
                                
                                Text("MiniMate needs your location to find nearby courses and update your position.")
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 32)
                            }

                            Button {
                                // Directs the user to the iOS Settings app
                                if let settingsUrl = URL(string: UIApplication.openSettingsURLString) {
                                    UIApplication.shared.open(settingsUrl)
                                }
                            } label: {
                                Text("Open Settings")
                                    .fontWeight(.semibold)
                                    .frame(width: 200, height: 50)
                                    .background(Color.blue)
                                    .foregroundStyle(.white)
                                    .cornerRadius(15)
                            }
                            .buttonStyle(.plain)
                            .transition(.scale.combined(with: .opacity))
                        }
                        .padding()
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                        .animation(.spring, value: locationHandler.hasLocationAccess)
                    }
                }
                .animation(.spring, value: course)
                .padding()
                .background(){
                    RoundedRectangle(cornerRadius: 25)
                        .subVsColor(makeColor: gameModel.course?.scoreCardColor)
                }
                .compositingGroup()
                .clipped()
            }
        }
    }
    
    var proStopper: some View {
        Group{
            if logic.shouldShowProStopper(
                isPro: KotlinBoolean(nonretainedObject: authModel.userModel?.isPro),
                gameCount: Int32(authModel.userModel?.gameIDs.count ?? 0)
            ) {
                Text("You’ve reached the free limit. Upgrade to Pro to store more than 2 games.")
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(){
                        RoundedRectangle(cornerRadius: 25)
                            .subVsColor(makeColor: gameModel.course?.scoreCardColor)
                    }
                    .compositingGroup()
            }
        }
    }
    
    var ad: some View {
        Group{
            if NetworkChecker.companion.shared.isConnected && !authModel.userModel!.isPro {
                VStack{
                    BannerAdView(adUnitID: "ca-app-pub-8261962597301587/6344452429") // Replace with real one later
                        .frame(height: 50)
                        .padding()
                }
                .background(){
                    RoundedRectangle(cornerRadius: 25)
                        .subVsColor(makeColor: gameModel.course?.scoreCardColor)
                }
                .compositingGroup()
            }
        }
    }
    
    @ViewBuilder
    var lastGameStats: some View {
        if showLastGameStats, let lastGame = gameManager.kotlin.latestGame() {
            
            Button {
                gameReview = lastGame
            } label: {
                SectionStatsView(
                    title: "Last Game",
                    spacing: 12,
                    makeColor: gameModel.course?.scoreCardColor
                ) {
                    let cardHeight: CGFloat = 90
                    
                    HStack(spacing: 12) {
                        // 2. Only show Winner if it's a multiplayer game
                        if lastGame.players.count > 1 {
                            PhotoIconView(
                                photoURL: gameManager.kotlin.winnerOfLatestGame()?.photoURL,
                                name: logic.formatWinnerName(name: gameManager.kotlin.winnerOfLatestGame()?.name),
                                ballColor: gameManager.kotlin.winnerOfLatestGame()?.ballColor,
                                imageSize: 30,
                                background: .yellow
                            )
                            .padding()
                            .frame(height: cardHeight)
                            .background{
                                RoundedRectangle(cornerRadius: 12)
                                    .subTwoVsColor(makeColor: gameModel.course?.scoreCardColor)
                            }
                        }
                        
                        StatCard(
                            title: "Your Strokes",
                            value: "\(gameManager.kotlin.usersScoreOfLatestGame())",
                            makeColor: gameModel.course?.scoreCardColor,
                            cornerRadius: 12,
                            cardHeight: cardHeight,
                            infoText: "The number of strokes you had last game."
                        )
                    }
                    
                    BarChartView(data: gameManager.kotlin.usersHolesOfLatestGame(), title: "Recap of Game")
                        .frame(height: 150)
                        .background(
                            RoundedRectangle(cornerRadius: 12)
                                .subTwoVsColor(makeColor: gameModel.course?.scoreCardColor)
                        )
                }
                .padding(.bottom)
                
            }
            .buttonStyle(.plain)
            .sheet(item: $gameReview) { game in
                GameReviewView(game: game, showBackToStatsButton: true)
                    .presentationDragIndicator(.visible)
            }
            .transition(.asymmetric(
                insertion: .move(edge: .bottom).combined(with: .opacity),
                removal: .opacity
            ))
            
        } else {
            LogoDefault(topPadding: 0)
        }
    }
    
    func gameModeButton(title: String, icon: String? = nil, color: Color, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                if let icon = icon {
                    Image(systemName: icon)
                }
                Text(title)
                    .fontWeight(.semibold)
            }
            .padding(10)
            .frame(maxWidth: .infinity, minHeight: 50)
            .background(RoundedRectangle(cornerRadius: 15).fill().foregroundStyle(color).opacity(disablePlaying ? 0.5 : 1))
            .foregroundColor(.white.opacity(disablePlaying ? 0.5 : 1))
        }
    }
}

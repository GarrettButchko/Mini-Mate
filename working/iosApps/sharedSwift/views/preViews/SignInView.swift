//
//  ContentView.swift
//  Sacred Truth
//
//  Created by Garrett Butchko on 5/28/25.
//

import SwiftUI
import _AuthenticationServices_SwiftUI
import FirebaseAuth
import SwiftData
import MarqueeText

#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif

struct SignInView: View {
    enum Field: Hashable { case email, password, confirm }
    
    @State var showEmailSignIn: Bool = false
    @State var errorMessage: (message: String?, type: Bool) = (nil, false)
    
    @State var email = ""
    @State var password = ""
    @State var confirmPassword = ""
    
    @Environment(\.modelContext) var context
    @Environment(\.colorScheme)  var colorScheme
    
    @EnvironmentObject var authModel : AuthViewModelSwift
    @EnvironmentObject var viewManager : ViewManagerSwift
    
    @State var guestGame: Game? = nil

    @StateObject private var keyboard = KeyboardObserver()
    @FocusState private var isTextFieldFocused: Field?
    
    var gradientColors: [Color] = [.blue, .green]

    private let characterLimit = 15
    
    var body: some View {
            ZStack {
                
                Rectangle()
                    .foregroundStyle(Gradient(colors: gradientColors))
                    .ignoresSafeArea()
                
                VStack(spacing: 10){
                    header
                    Spacer()
                }
                
                VStack {
                    Spacer()
                    card
                        .padding(.bottom, 16)
                }
                .ignoresSafeArea(.keyboard, edges: .bottom)
            }
            .onAppear {
                authModel.firebaseUser = nil
            }
            .ignoresSafeArea(.container, edges: .bottom)
        }
    
    var header: some View {
        HStack(alignment: .top){
            VStack(alignment: .leading){
                Text("Welcome to,")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .colorScheme(.dark)
                    
            #if MINIMATE
                Text("Mini Mate")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .colorScheme(.dark)
            #endif
                
            #if MANAGER
                Text("Mini Manager")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .colorScheme(.dark)
            #endif
            
            }
            
            Spacer()
            
            Image("logoOpp")
                .resizable()
                .scaledToFit()
                .frame(width: 75, height: 75)
                .colorScheme(.dark)
        }
        .padding(.horizontal)
    }
    
    var card: some View {
        Group {
            if !showEmailSignIn {
                StartButtons(
                    showEmailSignIn: $showEmailSignIn,
                    errorMessage: $errorMessage,
                    guestGame: $guestGame
                )
                .transition(.move(edge: .bottom).combined(with: .opacity))
            } else {
                EmailPasswordView(
                    showEmail: $showEmailSignIn,
                    email: $email,
                    password: $password,
                    confirmPassword: $confirmPassword,
                    guestGame: $guestGame,
                    keyboardHeight: keyboard.keyboardHeight,
                    isTextFieldFocused: $isTextFieldFocused
                )
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .frame(maxHeight: 600)
            }
        }
        .background(
            RoundedRectangle(cornerRadius: 35)
                .foregroundStyle(.ultraThinMaterial)
        )
        .clipShape(RoundedRectangle(cornerRadius: 35))
        .frame(maxWidth: 430)
        .animation(.bouncy.speed(1.5), value: showEmailSignIn)
        .padding()
    }
}


struct StartButtons: View {
    @Environment(\.modelContext) var context
    @Environment(\.colorScheme) var colorScheme
    #if MINIMATE
    @EnvironmentObject var gameModel: GameViewModelSwift
    #endif
    
    @Binding var showEmailSignIn: Bool
    @Binding var errorMessage: (message: String?, type: Bool)
    
    @State var showGuestAlert: Bool = false
    @State var guestName: String = ""
    @State var guestEmail: String = ""
    
    @Binding var guestGame: Game?
    
    @EnvironmentObject var authModel: AuthViewModelSwift
    @EnvironmentObject var viewManager: ViewManagerSwift
    
    var body: some View {
        VStack(spacing: 16){
            // Email / Password Button
            #if MINIMATE
            if let guestGame = guestGame {
                HStack{
                    VStack(alignment: .leading, spacing: 6) {
                        
                        Text("Save Guest-Play Game?")
                            .font(.headline)
                            .foregroundStyle(.mainOpp)
                        
                        Text("Sign in to save it to your profile.")
                            .font(.subheadline)
                            .foregroundStyle(.mainOpp.opacity(0.6))
                        
                       // Text("Game played on: \(guestGame.date.formatted(date: .abbreviated, time: .shortened))")
                           // .font(.caption)
                           // .foregroundStyle(.mainOpp.opacity(0.7))
                    }
                    
                    Spacer()
                }
                .padding(.vertical, 18)
                .padding(.horizontal, 22)
                .background{
                    RoundedRectangle(cornerRadius: 25)
                        .foregroundStyle(.ultraThinMaterial)
                }
            }
            
            Button {
                withAnimation {
                    showGuestAlert = true
                }
            } label: {
                ZStack {
                    RoundedRectangle(cornerRadius: 25)
                        .foregroundStyle(.ultraThinMaterial)
                    
                    HStack{
                        Image(systemName: "play.fill")
                        Text("Guest Play")
                    }
                    .font(.system(size: 18))
                    .foregroundStyle(.mainOpp)
                    .fontWeight(.semibold)
                }
                .frame(height: 50)
            }
            .alert("Welcome To MiniMate!", isPresented: $showGuestAlert) {
                
                TextField("Name", text: $guestName)
                    .characterLimit($guestName, maxLength: 18)
                
                
                TextField("Email (Optional)", text: $guestEmail)
                    .autocapitalization(.none)   // starts lowercase / no auto-cap
                    .keyboardType(.emailAddress)
                
                
                Button("Play") {
                    //gameModel.kotlinVM.createGame(guestData: GuestData(id: "guest-\(UUID().uuidString.prefix(6))", email: guestEmail == "" ? nil : guestEmail, name: guestName, ballColorDT: <#String?#>))
                    viewManager.kotlinVM.navigateToHost()
                }
                .disabled(
                    guestName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
                    ProfanityFilter().containsBlockedWord(text: guestName) ||
                    (
                        !guestEmail.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty &&
                        !guestEmail.isValidEmail
                    )
                )
                .tint(.blue)
                
                Button("Cancel", role: .cancel) {
                    guestName = ""
                    guestEmail = ""
                }
            } message: {
                Text("Name is required. Email is optional — used only for course analytics.")
            }
            .onAppear {
                Task {
                    do {
                        try guestGame = await LocalGameRepository().fetchGuestGame()
                    } catch {
                        print("Error loading guest game: \(error)")
                    }
                }
            }
            #endif
            
            Button {
                withAnimation {
                    showEmailSignIn = true
                }
            } label: {
                ZStack {
                    RoundedRectangle(cornerRadius: 25)
                        .foregroundStyle(.green)
                    
                    HStack{
                        Image(systemName: "envelope.fill")
                        Text("Sign in with Email")
                    }
                    .font(.system(size: 18))
                    .foregroundStyle(.white)
                    .fontWeight(.semibold)
                }
            }
            .frame(height: 50)

            Button {
                authModel.signInWithGoogle() { result in
                    authModel.kotlinVM.handleSignInResult(result: result) { message, type in
                        errorMessage = (message: message, type: type.boolValue)
                    } onClearGuestGame: { game in
                        guestGame = game
                    }
                }
            } label: {
                ZStack {
                    RoundedRectangle(cornerRadius: 25)
                        .frame(height: 50)
                        .foregroundStyle(.blue)
                    HStack{
                        Image("google")
                            .resizable()
                            .frame(width: 23, height: 22)
                            .background(Color.white)
                            .clipShape(Circle())
                        Text("Sign in with Google")
                            .font(.system(size: 18, weight: .semibold))
                            .foregroundStyle(.white)
                    }
                }
            }
            SignInWithAppleButton { request in
                authModel.kotlinVM.handleSignInWithAppleRequest(request: request)
            } onCompletion: { result in
                authModel.kotlinVM.handleSignInResult(result: result) { message, type in
                    errorMessage = (message: message, type: type.boolValue)
                } onClearGuestGame: { game in
                    guestGame = game
                }
            }
            .signInWithAppleButtonStyle(colorScheme == .light ? .black : .white)
            .frame(height: 50)
            .cornerRadius(25)
        }
        .padding()
    }
}



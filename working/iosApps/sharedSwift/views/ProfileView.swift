// ProfileView.swift
// MiniMate
//
// Updated to use UserModel and AuthViewModel

import SwiftUI
import FirebaseAuth
import AuthenticationServices
import SwiftData

#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif



struct ProfileView: View {
    @EnvironmentObject var viewManager: ViewManagerSwift
    @EnvironmentObject var authModel: AuthViewModelSwift
    
    @Binding var isSheetPresent: Bool
    @State var showLoginOverlay: Bool = false
    
    @State var password: String = ""
    @State var confirmPassword: String = ""
    
    @State private var showingPhotoPicker = false
    
    @State private var pickedImage: UIImage? = nil
    
    #if MINIMATE
    @State private var showPro: Bool = false
    #endif
    
    @StateObject private var viewModel = ProfileViewModelSwift()

    
    var body: some View {
        ZStack {
            VStack {
                // Header and drag indicator
                Capsule()
                    .frame(width: 38, height: 6)
                    .foregroundColor(.gray)
                    .padding(10)
                
                HStack {
                    Text("Profile")
                        .font(.title)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                        .padding(.leading, 16)
                    Spacer()
                    Text("Tap to change photo")
                        .font(.caption)
                    Button {
                        showingPhotoPicker = true
                    } label: {
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
                        } else {
                            Image("logoOpp")
                                .resizable()
                                .scaledToFill()
                                .frame(width: 40, height: 40)
                        }
                    }
                    .padding(.trailing, 30)
                }
                .sheet(isPresented: $showingPhotoPicker) {
                    PhotoPicker(image: $pickedImage)
                        .onChange(of: pickedImage) { old , newImage in
                            viewModel.kotlinVM.managePictureChange(newImage: newImage)
                        }
                }
                
                List {
                    // User Details Section
                    Section("User Details") {
                        if let user = authModel.userModel {
                            HStack {
                                Text("Name:")
                                Text(user.name)
                            }
                            
                            HStack {
                                Text("Email:")
                                Text(user.email ?? "")
                            }
                            
                            HStack {
                                Text("UID:")
                                Text(user.googleId)
                            }
                            
                            HStack {
                                Text("Pro:")
                                Text((user.isPro ? "Yes" : "Not Yet!"))
                                
                                #if MINIMATE
                                if !user.isPro {
                                    Spacer()
                                    Button("Get Pro Now!") {
                                        showPro = true
                                    }
                                    .padding(.horizontal)
                                }
                                #endif
                            }
                            
                        } else {
                            Text("User data not available.")
                        }
                    }
                    
                    // Account Management Section
                    Section("Account Management") {
                        
                        // Only allow edit/reset for non-social accounts
                        if let user = authModel.userModel{
                            Button("Edit Name") {
                                viewModel.kotlinVM.oldName = user.name
                                viewModel.editProfile = true
                            }
                            .alert("Edit Name", isPresented: $viewModel.editProfile) {
                                TextField("New Name", text: $viewModel.name)
                                    .characterLimit($viewModel.name, maxLength: 18)

                                Button("Change") {
                                    viewModel.kotlinVM.saveName()
                                }
                                .disabled(ProfanityFilter.shared.containsBlockedWord(text: viewModel.name) || viewModel.name.isEmpty)

                                Button("Cancel", role: .cancel) {
                                    viewModel.editProfile = false
                                }
                            }
                            if user.accountType.contains("email") && !user.accountType.contains("google") {
                                Button("Password Reset") {
                                    viewModel.kotlinVM.passwordReset(userModel: user)
                                }
                            }
                        }
                        
                        Button("Logout") {
                            isSheetPresent = false
                            viewModel.kotlinVM.logOut()
                        }
                        .foregroundColor(.red)
                        
                        Button("Delete Account") {
                            viewModel.kotlinVM.deleteAccount(userModel: authModel.userModel!)
                        }
                        .foregroundColor(.red)
                        .alert(item: $viewModel.activeDeleteAlert) { alertType in
                            switch alertType {
                            case .google:
                                return Alert(
                                    title: Text("Confirm Deletion"),
                                    message: Text("This will permanently delete your account."),
                                    primaryButton: .destructive(Text("Delete")) {
                                        // call Google deletion flow
                                        viewModel.googleReauthAndDelete(authModel: authModel, isSheetPresented: $isSheetPresent)
                                    },
                                    secondaryButton: .cancel()
                                )
                                
                            case .apple:
                                return Alert(
                                    title: Text("Confirm Deletion"),
                                    message: Text("This will permanently delete your account."),
                                    primaryButton: .destructive(Text("Delete")) {
                                        viewModel.startAppleReauthAndDelete(isSheetPresented: $isSheetPresent)
                                    },
                                    secondaryButton: .cancel()
                                )
                                
                            case .email:
                                return Alert(
                                    title: Text("Confirm Deletion"),
                                    message: Text("This will permanently delete your account."),
                                    primaryButton: .destructive(Text("Delete")) {
                                        showLoginOverlay = true
                                    },
                                    secondaryButton: .cancel()
                                )
                            }
                        }
                    }
                    
                    // Bot Message Section
                    if !viewModel.botMessage.isEmpty {
                        Section("Message") {
                            Text(viewModel.botMessage)
                                .foregroundColor(viewModel.isRed ? .red : .green)
                        }
                    }
                }
                .onAppear {
                    if let user = authModel.userModel {
                        viewModel.name = user.name
                        viewModel.email = user.email ?? ""
                    }
                }
            }
            .alert("Confirm Deletion", isPresented: $showLoginOverlay) {
                SecureField("Password", text: $password)
                SecureField("Confirm Password", text: $confirmPassword)
                
                Button("Delete", role: .destructive) {
                    viewModel.emailReauthAndDelete(
                        emailInput: authModel.userModel!.email!,
                        passwordInput: password, isSheetPresented: $isSheetPresent
                    )
                }
                .disabled((password != confirmPassword) || password == "" || confirmPassword == "")

                Button("Cancel", role: .cancel) {
                    password = ""
                    confirmPassword = ""
                }
            } message: {
                Text("This will permanently delete your account.")
            }
            #if MINIMATE
            .sheet(isPresented: $showPro) {
                ProView(showSheet: $showPro)
            }
            #endif
        }
    }
}


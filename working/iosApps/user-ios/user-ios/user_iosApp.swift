//
//  user_iosApp.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/13/26.
//

import SwiftUI
import FirebaseCore
import shared_user
import GoogleSignIn


class AppDelegate: NSObject, UIApplicationDelegate {
  func application(_ application: UIApplication,
                   didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
    FirebaseApp.configure()

    startKoinIos()

    return true
  }

  func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
    return GIDSignIn.sharedInstance.handle(url)
  }
}

@main
struct user_iosApp: App {
    // register app delegate for Firebase setup
      @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate


    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

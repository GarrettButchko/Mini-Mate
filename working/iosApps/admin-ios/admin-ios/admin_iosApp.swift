//
//  admin_iosApp.swift
//  admin-ios
//
//  Created by Garrett Butchko on 3/13/26.
//

import SwiftUI
import FirebaseCore
import shared_admin


class AppDelegate: NSObject, UIApplicationDelegate {
  func application(_ application: UIApplication,
                   didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
    FirebaseApp.configure()

      startKoinIos()

    return true
  }
}

@main
struct admin_iosApp: App {
    // register app delegate for Firebase setup
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

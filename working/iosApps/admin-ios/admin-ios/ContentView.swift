//
//  ContentView.swift
//  admin-ios

//
//  Created by Garrett Butchko on 3/13/26.
//

import SwiftUI
import shared

struct ContentView: View {
    let greeting = shared.Greeting()
    
    let course = shared.Course(
        id: "course_001",
        name: "Sunset Valley",
        password: "secure",
        logo: nil,
        scoreCardColorDT: nil,
        courseColorsDT: [],
        customPar: false,
        numHoles: 18,
        pars: [],
        socialLinks: [shared.SocialLink(id: "blah blah", platform: .facebook, url: "url here")], // Kotlin List becomes Swift Array
        latitude: 34.05,
        longitude: -118.24,
        isSeasonal: nil,
        indoor: nil,
        leaderBoardActive: false,
        tier: 1,
        adminIDs: [],
        isClaimed: false,
        isSupported: false,
        customAdActive: false,
        adTitle: nil,
        adDescription: nil,
        adLink: nil,
        adImage: nil,
        adClicks: nil
    )

    var body: some View {
        VStack {
            Text("Admin: \(greeting.greet())")
            
            course.socialLinks.first?.platformImage
                .resizable()
                .frame(width: 40, height: 40)
        }
        .padding()
    }
}

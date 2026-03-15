//
//  SocialLink+Extension.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/14/26.
//
import shared
import SwiftUI

extension SocialLink {
    var platformImage: Image {
        switch platform {
        case .instagram:
            return Image("instagram")
        case .facebook:
            return Image("facebook")
        case .tiktok:
            return Image("tiktok")
        case .youtube:
            return Image("youtube")
        case .website:
            return Image(systemName: "globe")
        default:
            return Image(systemName: "globe")
        }
    }
}
    

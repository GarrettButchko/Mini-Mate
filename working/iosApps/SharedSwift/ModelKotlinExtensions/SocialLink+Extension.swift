//
//  SocialLink+Extension.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/14/26.
//
#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif
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
        }
    }
}
    

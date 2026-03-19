//
//  Player+UI.swift
//  MiniMate
//
//  Created by Garrett Butchko on 3/8/26.
//
import SwiftUI
#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif

extension Player {
    var ballColor: Color {
        ballColorDT?.toColor() ?? .mainOpp
    }
}

extension PlayerDTO {
    var ballColor: Color {
        ballColorDT?.toColor() ?? .mainOpp
    }
}

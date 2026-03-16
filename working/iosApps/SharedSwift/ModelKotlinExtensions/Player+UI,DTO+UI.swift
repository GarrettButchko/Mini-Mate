//
//  Player+UI.swift
//  MiniMate
//
//  Created by Garrett Butchko on 3/8/26.
//
import SwiftUI
import shared

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



//
//  User+UserModel+UI.swift
//  MiniMate
//
//  Created by Garrett Butchko on 3/11/26.
//

import SwiftUI
import shared

extension UserDTO {
    var ballColor: Color {
        ballColorDT?.toColor() ?? .mainOpp
    }
}

extension User{
    var ballColor: Color {
        ballColorDT?.toColor() ?? .mainOpp
    }
}



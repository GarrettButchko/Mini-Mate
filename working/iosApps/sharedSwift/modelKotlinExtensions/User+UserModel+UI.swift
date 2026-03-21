//
//  User+UserModel+UI.swift
//  MiniMate
//
//  Created by Garrett Butchko on 3/11/26.
//

import SwiftUI
#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif

extension UserDTO {
    var ballColor: Color {
        ballColorDT?.toColor() ?? .mainOpp
    }
}

extension UserModel{
    var ballColor: Color {
        ballColorDT?.toColor() ?? .mainOpp
    }
}

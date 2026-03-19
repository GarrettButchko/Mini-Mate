//
//  Course+UI.swift
//  MiniMate
//
//  Created by Garrett Butchko on 12/19/25.
//

import SwiftUI
#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif

extension Course {
    var scoreCardColor: Color? {
        guard let value = scoreCardColorDT else { return nil }
        return value.toColor()?.opacity(0.4)
    }
    
    var courseColors: [Color]? {
        guard let values = courseColorsDT else { return nil }
        let colors = values.compactMap { $0.toColor() }
        return colors.isEmpty ? nil : colors
    }
}

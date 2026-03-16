//
// Created by Garrett Butchko on 3/15/26.
//

import Foundation
import SwiftUI


extension String {
    func toColor() -> Color? {
        let lowercased = self.lowercased()
        // Named color map
        let map: [String: Color] = [
            "red": .red,
            "orange": .orange,
            "yellow": .yellow,
            "green": .green,
            "blue": .blue,
            "indigo": .indigo,
            "purple": .purple,
            "pink": .pink,
            "cyan": .cyan,
            "brown": .brown
        ]

        return map[lowercased]
    }
}
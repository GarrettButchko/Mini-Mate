import Foundation

#if MINIMATE
import shared_user
#elseif MANAGER
import shared_admin
#endif

// MARK: - Identifiable Conformance for Kotlin Models

extension Course: @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension SocialLink: @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension Game: @retroactive Identifiable {
    public var newId: String {
        get { self.id }
        set { self.id = newValue }
    }
}

extension GameDTO: @retroactive Identifiable {
    public var newId: String {
        get { self.id }
        set { /* id is val in GameDTO */ }
    }
}

extension Hole: @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension HoleDTO: @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension Player: @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension PlayerDTO: @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension SmallCourse: @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension UserModel: @retroactive Identifiable {
    public var newId: String {
        return self.googleId
    }
}

extension UserDTO: @retroactive Identifiable {
    public var newId: String {
        return self.googleId
    }
}

extension MapItemDTO: @retroactive Identifiable {
    public var newId: String {
        return self.placeID ?? UUID().uuidString
    }
}

extension DeleteAlertType: @retroactive Identifiable {
    public var id: Int {
        switch self {
        case .google: return 0
        case .apple:  return 1
        case .email:  return 2
        }
    }
}

#if MANAGER
extension PlayerActivity: @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension GameDurationActivity : @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension HoleHeatmapData : @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension ColorDeleteTarget: @retroactive Identifiable {
    public var id: Int {
        if let courseColor = self as? ColorDeleteTarget.CourseColor {
            return Int(courseColor.index)
        } else {
            return -1
        }
    }
}

extension SocialPlatform: @retroactive Identifiable {
    public var id: Int {
        self.hashValue
    }
    
    var displayName: String {
        return self.name.lowercased().capitalized
    }
}

extension LeaderboardEntry: @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension HoleDifficultyData: @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}

extension HourData: @retroactive Identifiable {
    public var newId: String {
        return self.id
    }
}
#endif

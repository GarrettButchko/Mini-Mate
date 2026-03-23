import Foundation

#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
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
        return self.id
    }
}

extension GameDTO: @retroactive Identifiable {
    public var newId: String {
        return self.id
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

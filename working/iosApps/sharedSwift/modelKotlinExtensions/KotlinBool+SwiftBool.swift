//
//  KotlinBool+SwiftBool.swift
//  user-ios
//
//  Created by Garrett Butchko on 3/23/26.
//


#if canImport(shared_user)
import shared_user
#elseif canImport(shared_admin)
import shared_admin
#endif

extension Bool {
    /// Converts a Swift Bool? to a Kotlin-compatible Boolean object
    var asKotlinBoolean: KotlinBoolean {
        guard let self = self else { return nil }
        return KotlinBoolean(bool: self)
    }
}

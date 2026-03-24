package com.garrettbutchko.minimate.datamodels

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.gitlive.firebase.firestore.Timestamp

@Entity
data class UserModel(
    @PrimaryKey
    var googleId: String,
    var appleId: String? = null,
    var name: String,
    var photoURL: String? = null,
    var email: String? = null,
    var ballColorDT: String? = null,
    var isPro: Boolean = false,
    var gameIDs: List<String> = emptyList(),
    var lastUpdated: Timestamp = Timestamp.now(),
    var accountType: List<String> = emptyList(),
    var adminCourses: List<String> = emptyList()
) {
    fun toDTO(): UserDTO {
        return UserDTO(
            googleId = googleId,
            appleId = appleId,
            name = name,
            photoURL = photoURL,
            email = email,
            ballColorDT = ballColorDT,
            isPro = isPro,
            gameIDs = gameIDs,
            lastUpdated = lastUpdated,
            accountType = accountType,
            adminCourses = adminCourses
        )
    }
}

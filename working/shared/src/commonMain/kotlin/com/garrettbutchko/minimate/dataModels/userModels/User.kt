package com.garrettbutchko.minimate.datamodels

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.gitlive.firebase.firestore.Timestamp

@Entity
data class User(
    @PrimaryKey
    val googleId: String,
    val appleId: String? = null,
    val name: String,
    val photoURL: String? = null,
    val email: String? = null,
    val ballColorDT: String? = null,
    val isPro: Boolean = false,
    val gameIDs: List<String> = emptyList(),
    val lastUpdated: Timestamp = Timestamp.now(),
    val accountType: List<String> = emptyList(),
    val adminCourses: List<String> = emptyList()
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

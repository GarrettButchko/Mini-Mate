package com.garrettbutchko.minimate.dataModels.playerModels

import com.garrettbutchko.minimate.dataModels.holeModels.HoleDTO
import kotlinx.serialization.Serializable

@Serializable
data class PlayerDTO(
    val id: String,
    val userId: String,
    val name: String,
    val photoURL: String? = null,
    val email: String? = null,
    val ballColorDT: String? = null,
    val inGame: Boolean = false,
    val totalStrokes: Int = 0,
    val holes: List<HoleDTO> = emptyList()
) {
    fun toPlayer(): Player {
        return Player(
            id = id,
            userId = userId,
            name = name,
            photoURL = photoURL,
            email = email,
            ballColorDT = ballColorDT,
            inGame = inGame,
            holes = holes.map { it.toHole() }
        )
    }

    fun convertToLBREP(): LeaderboardEntry? {
        return if (email != null) {
            LeaderboardEntry(id = id, userId = userId, name = name, photoURL = photoURL, ballColorDT = ballColorDT, totalStrokes = totalStrokes, email = email)
        } else {
            null
        }
    }
}


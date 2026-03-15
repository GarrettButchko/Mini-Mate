package com.garrettbutchko.minimate.datamodels

import com.garrettbutchko.minimate.datamodels.Player
import com.garrettbutchko.minimate.DataModels.HoleModels.HoleDTO
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
}

package com.garrettbutchko.minimate.dataModels.playerModels

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import com.garrettbutchko.minimate.dataModels.holeModels.Hole
import com.garrettbutchko.minimate.generateUUID


@Entity
data class Player(
    @PrimaryKey
    val id: String = generateUUID(),
    val userId: String,
    val name: String,
    val photoURL: String? = null,
    val email: String? = null,
    val ballColorDT: String? = null,
    val inGame: Boolean = false,
    val holes: List<Hole> = emptyList() // Requires Room TypeConverter
) {
    @Ignore
    val totalStrokes: Int = holes.sumOf { it.strokes }

    @Ignore
    val incomplete: Boolean = holes.any { it.strokes == 0 }

    fun toDTO(): PlayerDTO {
        return PlayerDTO(
            id = id,
            userId = userId,
            name = name,
            photoURL = photoURL,
            email = email,
            ballColorDT = ballColorDT,
            inGame = inGame,
            totalStrokes = totalStrokes,
            holes = holes.map { it.toDTO() }
        )
    }
}

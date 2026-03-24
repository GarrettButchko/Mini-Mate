package com.garrettbutchko.minimate.dataModels.playerModels

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Ignore
import com.garrettbutchko.minimate.dataModels.holeModels.Hole
import com.garrettbutchko.minimate.generateUUID
import kotlinx.serialization.Serializable


@Serializable
@Entity
data class Player(
    @PrimaryKey
    val id: String = generateUUID(),
    var userId: String,
    var name: String,
    var photoURL: String? = null,
    var email: String? = null,
    var ballColorDT: String? = null,
    var inGame: Boolean = false,
    var holes: List<Hole> = emptyList() // Requires Room TypeConverter
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

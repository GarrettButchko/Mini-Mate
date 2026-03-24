package com.garrettbutchko.minimate.dataModels.holeModels

import com.garrettbutchko.minimate.generateUUID // Assuming you have a helper for this

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class Hole(
    @PrimaryKey
    val id: String = generateUUID(), // Default value like your Swift code
    var number: Int,
    var strokes: Int = 0
) {
    // Porting the DTO conversion logic
    fun toDTO(): HoleDTO {
        return HoleDTO(
            id = id,
            number = number,
            strokes = strokes
        )
    }
}
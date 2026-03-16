package com.garrettbutchko.minimate.datamodels

import com.garrettbutchko.minimate.generateUUID // Assuming you have a helper for this
import com.garrettbutchko.minimate.DataModels.HoleModels.HoleDTO

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Hole(
    @PrimaryKey
    val id: String = generateUUID(), // Default value like your Swift code
    val number: Int,
    val strokes: Int = 0
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
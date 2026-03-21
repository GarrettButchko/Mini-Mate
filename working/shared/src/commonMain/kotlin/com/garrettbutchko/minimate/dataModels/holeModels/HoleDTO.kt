package com.garrettbutchko.minimate.dataModels.holeModels

import kotlinx.serialization.Serializable

@Serializable
data class HoleDTO(
    var id: String,
    var number: Int,
    var strokes: Int
){
   fun toHole(): Hole {
        return Hole(
            id = id,
            number = number,
            strokes = strokes
        )
    }
}
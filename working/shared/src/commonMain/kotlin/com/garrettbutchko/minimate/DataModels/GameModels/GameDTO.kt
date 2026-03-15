package com.garrettbutchko.minimate.datamodels

import com.garrettbutchko.minimate.database.Game
import com.garrettbutchko.minimate.datamodels.PlayerDTO
import kotlinx.serialization.Serializable
import dev.gitlive.firebase.firestore.Timestamp

@Serializable
data class GameDTO(
    val id: String,
    val hostUserId: String,
    val date: Timestamp,
    val completed: Boolean = false,
    val numberOfHoles: Int,
    val started: Boolean = false,
    val dismissed: Boolean = false,
    val live: Boolean = false,
    val lastUpdated: Timestamp,
    val courseID: String? = null,
    val locationName: String? = null,
    val startTime: Timestamp,
    val endTime: Timestamp,
    val players: List<PlayerDTO> = emptyList()
){
    fun toGame(): Game {
        return Game(
            id = id,
            hostUserId = hostUserId,
            date = date,
            completed = completed,
            numberOfHoles = numberOfHoles,
            started = started,
            dismissed = dismissed,
            live = live,
            lastUpdated = lastUpdated,
            courseID = courseID,
            locationName = locationName,
            startTime = startTime,
            endTime = endTime,
            players = players.map { it.toPlayer() }
        )
    }
}
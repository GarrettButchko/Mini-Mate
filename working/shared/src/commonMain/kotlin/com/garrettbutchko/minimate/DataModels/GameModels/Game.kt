package com.garrettbutchko.minimate.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.gitlive.firebase.firestore.Timestamp
import com.garrettbutchko.minimate.datamodels.GameDTO
import com.garrettbutchko.minimate.datamodels.Player

@Entity
data class Game(
    @PrimaryKey
    val id: String = "",
    val hostUserId: String = "",
    val date: Timestamp = Timestamp.now(),
    val completed: Boolean = false,
    val numberOfHoles: Int = 18,
    val started: Boolean = false,
    val dismissed: Boolean = false,
    val live: Boolean = false,
    val lastUpdated: Timestamp = Timestamp.now(),
    val courseID: String? = null,
    val locationName: String? = null,
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp = Timestamp.now(),
    val players: List<Player> = emptyList() // Requires TypeConverter
) {
    // Keep your logic here! It works fine in Room entities
    val holeInOneLastHole: Boolean
        get() = players.any { player ->
            player.holes.any { hole -> hole.number == 18 && hole.strokes == 1 }
        }
    val isActive: Boolean
        get() = started && !completed && !dismissed

    fun toDTO(): GameDTO {
        return GameDTO(
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
            players = players.map { it.toDTO() }, // Use 'it' for the closure
            locationName = locationName,
            startTime = startTime,
            endTime = endTime
        )
    }
}
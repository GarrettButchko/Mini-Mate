package com.garrettbutchko.minimate.dataModels.gameModels

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.gitlive.firebase.firestore.Timestamp
import com.garrettbutchko.minimate.dataModels.playerModels.Player

@Entity
data class Game(
    @PrimaryKey
    var id: String = "",
    var hostUserId: String = "",
    var date: Timestamp = Timestamp.now(),
    var completed: Boolean = false,
    var numberOfHoles: Int = 18,
    var started: Boolean = false,
    var dismissed: Boolean = false,
    var live: Boolean = false,
    var lastUpdated: Timestamp = Timestamp.now(),
    var courseID: String? = null,
    var locationName: String? = null,
    var startTime: Timestamp = Timestamp.now(),
    var endTime: Timestamp = Timestamp.now(),
    var players: List<Player> = emptyList() // Requires TypeConverter
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
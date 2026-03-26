package com.garrettbutchko.minimate.repositories

import com.garrettbutchko.minimate.dataModels.gameModels.Game
import com.garrettbutchko.minimate.dataModels.playerModels.PlayerDTO
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class LiveGameDTO(
    val id: String = "",
    val hostUserId: String = "",
    val date: Long = 0L,
    val completed: Boolean = false,
    val numberOfHoles: Int = 18,
    val started: Boolean = false,
    val dismissed: Boolean = false,
    val live: Boolean = false,
    val lastUpdated: Long = 0L,
    val courseID: String? = null,
    val locationName: String? = null,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val players: List<PlayerDTO> = emptyList()
) {
    fun toGame(): Game {
        return Game(
            id = id,
            hostUserId = hostUserId,
            date = Timestamp(date / 1000, ((date % 1000) * 1000000).toInt()),
            completed = completed,
            numberOfHoles = numberOfHoles,
            started = started,
            dismissed = dismissed,
            live = live,
            lastUpdated = Timestamp(lastUpdated / 1000, ((lastUpdated % 1000) * 1000000).toInt()),
            courseID = courseID,
            locationName = locationName,
            startTime = Timestamp(startTime / 1000, ((startTime % 1000) * 1000000).toInt()),
            endTime = Timestamp(endTime / 1000, ((endTime % 1000) * 1000000).toInt()),
            players = players.map { it.toPlayer() }
        )
    }
}

fun Game.toLiveDTO(): LiveGameDTO {
    return LiveGameDTO(
        id = id,
        hostUserId = hostUserId,
        date = date.seconds * 1000 + date.nanoseconds / 1000000,
        completed = completed,
        numberOfHoles = numberOfHoles,
        started = started,
        dismissed = dismissed,
        live = live,
        lastUpdated = lastUpdated.seconds * 1000 + lastUpdated.nanoseconds / 1000000,
        courseID = courseID,
        locationName = locationName,
        startTime = startTime.seconds * 1000 + startTime.nanoseconds / 1000000,
        endTime = endTime.seconds * 1000 + endTime.nanoseconds / 1000000,
        players = players.map { it.toDTO() }
    )
}

/** Handles Realtime Database operations for Game objects */
class LiveGameRepository {

    private val dbRef = Firebase.database.reference("live_games")
    // Using a single-threaded dispatcher (limitedParallelism(1)) to ensure that operations
    // (like add followed by delete) are processed in the correct order on the client.
    private val scope = CoroutineScope(Dispatchers.IO.limitedParallelism(1) + SupervisorJob())

    // MARK: - Add or Update Game
    fun addOrUpdateGame(game: Game, completion: (Boolean) -> Unit) {
        scope.launch {
            try {
                val dto = game.toLiveDTO()
                dbRef.child(game.id).setValue(dto)
                withContext(Dispatchers.Main) {
                    completion(true)
                }
            } catch (e: Exception) {
                println("❌ Encoding game error: ${e.message}")
                withContext(Dispatchers.Main) {
                    completion(false)
                }
            }
        }
    }

    // MARK: - Fetch Game by ID
    fun fetchGame(id: String, completion: (Game?) -> Unit) {
        scope.launch {
            try {
                val snapshot = dbRef.child(id).valueEvents.firstOrNull()
                if (snapshot != null && snapshot.exists) {
                    val dto = snapshot.value<LiveGameDTO>()
                    val model = dto.toGame()
                    withContext(Dispatchers.Main) {
                        completion(model)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        completion(null)
                    }
                }
            } catch (e: Exception) {
                println("❌ Decoding game error: ${e.message}")
                withContext(Dispatchers.Main) {
                    completion(null)
                }
            }
        }
    }

    // MARK: - Delete Game
    fun deleteGame(id: String, completion: (Boolean) -> Unit) {
        scope.launch {
            try {
                dbRef.child(id).removeValue()
                withContext(Dispatchers.Main) {
                    completion(true)
                }
            } catch (e: Exception) {
                println("❌ Deleting game error: ${e.message}")
                withContext(Dispatchers.Main) {
                    completion(false)
                }
            }
        }
    }
}

package com.garrettbutchko.minimate.repositories

import com.garrettbutchko.minimate.datamodels.Game
import com.garrettbutchko.minimate.datamodels.GameDTO
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Handles Realtime Database operations for Game objects */
class LiveGameRepository {

    private val dbRef = Firebase.database.reference("live_games")
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // MARK: - Add or Update Game
    fun addOrUpdateGame(game: Game, completion: (Boolean) -> Unit) {
        scope.launch {
            try {
                val dto = game.toDTO()
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
                    val dto = snapshot.value<GameDTO>()
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

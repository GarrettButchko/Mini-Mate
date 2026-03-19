package com.garrettbutchko.minimate.repositories

import com.garrettbutchko.minimate.repositories.gameRepos.LocalGameRepository
import com.garrettbutchko.minimate.datamodels.Game
import com.garrettbutchko.minimate.datamodels.GameDTO
import com.garrettbutchko.minimate.repositories.gameRepos.RemoteGameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class UnifiedGameRepository(
    private val local: LocalGameRepository,
    private val remote: RemoteGameRepository
) {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun saveAllLocally(gameIds: List<String>, completion: (Boolean) -> Unit) {
        repositoryScope.launch {
            try {
                val games = fetchAll(gameIds)
                val entities = games.map { it.toGame() }
                val success = local.save(entities)
                withContext(Dispatchers.Main) {
                    completion(success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    completion(false)
                }
            }
        }
    }

    fun save(game: Game, completion: (Boolean, Boolean) -> Unit) {
        repositoryScope.launch {
            val localDeferred = async { local.save(game) }
            val remoteDeferred = async { remote.save(game).getOrDefault(false) }

            val localSuccess = localDeferred.await()
            val remoteSuccess = remoteDeferred.await()

            withContext(Dispatchers.Main) {
                completion(localSuccess, remoteSuccess)
            }
        }
    }

    suspend fun fetch(id: String): GameDTO? {
        // Try local first
        val localGame = local.fetch(id)
        if (localGame != null) {
            return localGame.toDTO()
        }
        // Then remote
        return remote.fetch(id)
    }

    suspend fun fetchAll(ids: List<String>): List<GameDTO> = coroutineScope {
        val localGames = local.fetchAll(ids).map { it.toDTO() }

        try {
            // Use withTimeout to mimic the Swift timeout behavior
            val remoteGames = withTimeoutOrNull(5000L) {
                remote.fetchAll(ids)
            } ?: emptyList()

            // Merge logic (preserving order of IDs and preferring remote if available)
            val combinedMap = (localGames + remoteGames).associateBy { it.id }
            ids.mapNotNull { combinedMap[it] }
        } catch (e: Exception) {
            localGames
        }
    }
}
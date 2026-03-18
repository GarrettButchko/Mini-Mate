package com.garrettbutchko.minimate.Repositories.GameRepos

import co.touchlab.kermit.Logger
import com.garrettbutchko.minimate.Room.Dao.GameDao
import com.garrettbutchko.minimate.database.Game

class LocalGameRepository(private val gameDao: GameDao) {

    // --- SAVING ---
    private val log = Logger.withTag("LocalGameRepo")

    suspend fun save(game: Game): Boolean {
        return try {
            gameDao.save(game)
            true
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to save game locally" }
            false
        }
    }

    suspend fun save(games: List<Game>): Boolean {
        return try {
            gameDao.save(games)
            true
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to save games locally" }
            false
        }
    }

    // --- FETCHING ---

    suspend fun fetch(id: String): Game? {
        return try {
            gameDao.fetch(id)
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to fetch locally by id: $id" }
            null
        }
    }

    /**
     * Replaces the Swift fetchGuestGame logic.
     * Uses the optimized SQL subquery to find orphaned guest games.
     */
    suspend fun fetchGuestGame(): Game? {
        return try {
            gameDao.fetchGuestGame()
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to fetch guest game safely" }
            null
        }
    }

    suspend fun fetchAll(ids: List<String>): List<Game> {
        return try {
            gameDao.fetchAll(ids)
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to fetch games" }
            emptyList()
        }
    }

    // --- MISSING IDS LOGIC ---

    suspend fun getMissingLocalGameIDs(remoteIds: List<String>): List<String> {
        val existingGames = fetchAll(remoteIds)
        val localIDs = existingGames.map { it.id }.toSet()
        return remoteIds.filter { id -> !localIDs.contains(id) }
    }

    // --- DELETING ---

    suspend fun delete(id: String): Boolean {
        return try {
            gameDao.delete(id)
            true
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to delete locally: $id" }
            false
        }
    }

    suspend fun deleteByIds(ids: List<String>): Boolean {
        return try {
            gameDao.deleteByIds(ids)
            true
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to delete multiple games" }
            false
        }
    }

    // --- CLEANUP ---

    /**
     * Replaces deleteAllUnusedGames.
     * Runs a single SQL command to delete games not referenced by any User.
     */
    suspend fun deleteAllUnusedGames(): Int {
        return try {
            val count = gameDao.deleteAllUnusedGames()
            log.d("✅ Deleted $count unused games")
            count
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to clean unused games"}
            0
        }
    }
}
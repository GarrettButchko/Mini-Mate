package com.garrettbutchko.minimate.Room.Dao

import androidx.room.*
import com.garrettbutchko.minimate.database.Game

@Dao
interface GameDao {

    // --- SAVING ---
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun save(game: Game)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun save(games: List<Game>)

    // --- FETCHING ---
    @Query("SELECT * FROM Game WHERE id = :id LIMIT 1")
    suspend fun fetch(id: String): Game?

    @Query("SELECT * FROM Game WHERE id IN (:ids)")
    suspend fun fetchAll(ids: List<String>): List<Game>

    @Query("SELECT * FROM Game")
    suspend fun fetchAllRaw(): List<Game>

    @Query("""
        SELECT * FROM Game 
        WHERE hostUserId LIKE '%guest%' 
        AND NOT EXISTS (
            SELECT 1 FROM User 
            WHERE gameIDs LIKE '%' || Game.id || '%'
        ) 
        LIMIT 1
    """)
    suspend fun fetchGuestGame(): Game?

    // --- DELETING ---
    @Query("DELETE FROM Game WHERE id = :id")
    suspend fun delete(id: String)

    @Delete
    suspend fun deleteAll(games: List<Game>)

    @Query("DELETE FROM Game WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM Game WHERE hostUserId LIKE '%guest%'")
    suspend fun deleteGuestGames()

    // --- CLEANUP (Delete Unused Games) ---
    @Query("""
        DELETE FROM Game 
        WHERE id NOT IN (SELECT DISTINCT gameIDs FROM User)
    """)
    suspend fun deleteAllUnusedGames(): Int
}
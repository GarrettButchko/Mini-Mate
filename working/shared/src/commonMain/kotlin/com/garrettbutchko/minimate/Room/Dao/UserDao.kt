package com.garrettbutchko.minimate.Room.Dao

import androidx.room.*
import com.garrettbutchko.minimate.datamodels.User

@Dao
interface UserDao {

    // --- FETCHING ---
    @Query("SELECT * FROM User WHERE googleId = :id LIMIT 1")
    suspend fun fetch(id: String): User?

    // --- SAVING ---
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun save(model: User)

    // --- DELETING ---
    @Query("DELETE FROM User WHERE googleId = :id")
    suspend fun delete(id: String)
}
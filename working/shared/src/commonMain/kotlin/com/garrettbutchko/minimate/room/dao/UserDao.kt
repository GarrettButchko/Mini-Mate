package com.garrettbutchko.minimate.room.dao

import androidx.room.*
import com.garrettbutchko.minimate.datamodels.UserModel

@Dao
interface UserDao {

    // --- FETCHING ---
    @Query("SELECT * FROM UserModel WHERE googleId = :id LIMIT 1")
    suspend fun fetch(id: String): UserModel?

    // --- SAVING ---
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun save(model: UserModel)

    // --- DELETING ---
    @Query("DELETE FROM UserModel WHERE googleId = :id")
    suspend fun delete(id: String)
}

package com.garrettbutchko.minimate.Room

import androidx.room.TypeConverter
import dev.gitlive.firebase.firestore.Timestamp
import com.garrettbutchko.minimate.datamodels.Player
import kotlinx.serialization.json.Json

class RoomConverters {
    private val json = Json { ignoreUnknownKeys = true }

    // Timestamps
    @TypeConverter
    fun fromTimestamp(value: Timestamp?): Long? = value?.seconds

    @TypeConverter
    fun toTimestamp(value: Long?): Timestamp? = value?.let {
        Timestamp(it, 0)
    }

    // Players List
    @TypeConverter
    fun fromPlayerList(value: List<Player>): String = json.encodeToString(value)

    @TypeConverter
    fun toPlayerList(value: String): List<Player> = json.decodeFromString(value)
}
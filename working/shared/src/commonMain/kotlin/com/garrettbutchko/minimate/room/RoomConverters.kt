package com.garrettbutchko.minimate.room

import androidx.room.TypeConverter
import com.garrettbutchko.minimate.datamodels.Hole
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

    // String List
    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)

    // Hole List
    @TypeConverter
    fun fromHoleList(value: List<Hole>): String = json.encodeToString(value)

    @TypeConverter
    fun toHoleList(value: String): List<Hole> = json.decodeFromString(value)

    // Players List
    @TypeConverter
    fun fromPlayerList(value: List<Player>): String = json.encodeToString(value)

    @TypeConverter
    fun toPlayerList(value: String): List<Player> = json.decodeFromString(value)
}

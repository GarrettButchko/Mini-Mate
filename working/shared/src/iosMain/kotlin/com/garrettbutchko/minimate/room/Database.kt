package com.garrettbutchko.minimate.room

import androidx.room.RoomDatabase
import androidx.room.Room
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = documentDirectory() + "/minimate.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
        factory = { AppDatabaseConstructor.initialize() }
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}

@file:JvmName("DatabaseAndroid")
package com.garrettbutchko.minimate.room

import android.app.Application
import android.content.Context
import androidx.room.*

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("minimate.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

actual object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    actual override fun initialize(): AppDatabase {
        return getRoomDatabase(getDatabaseBuilder(
            context = Application()
        ))
    }
}

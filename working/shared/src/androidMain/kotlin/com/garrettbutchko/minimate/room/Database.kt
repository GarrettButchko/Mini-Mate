@file:JvmName("DatabaseAndroid")
package com.garrettbutchko.minimate.room

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

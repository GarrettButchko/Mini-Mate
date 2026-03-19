package com.garrettbutchko.minimate.di

import org.koin.dsl.module

import android.content.Context
import androidx.room.Room
import com.garrettbutchko.minimate.room.AppDatabase
import com.garrettbutchko.minimate.room.getRoomDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module

actual val platformModule: Module = module {
    single<AppDatabase> {
        // 1. Get the Android Context provided during startKoin
        val context: Context = androidContext()
        // 2. Define the database file path
        val dbFile = context.getDatabasePath("minimate.db")

        // 3. Create the Room Builder
        val builder = Room.databaseBuilder<AppDatabase>(
            context = context,
            name = dbFile.absolutePath
        )
        // 4. Use your common helper to apply the driver and build it
        getRoomDatabase(builder)
    }
}
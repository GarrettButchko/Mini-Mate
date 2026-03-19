package com.garrettbutchko.minimate.di
import androidx.room.Room
import com.garrettbutchko.minimate.Room.AppDatabase
import com.garrettbutchko.minimate.Room.AppDatabaseConstructor
import com.garrettbutchko.minimate.Room.getRoomDatabase
import platform.Foundation.NSHomeDirectory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AppDatabase> {
        // 1. Define the path in the iOS Documents directory
        val dbFilePath = NSHomeDirectory() + "/minimate.db"
        // 2. Create the Room Builder
        // Note: iOS requires the 'factory' parameter for the generated implementation
        val builder = Room.databaseBuilder<AppDatabase>(
            name = dbFilePath,
            factory = { AppDatabaseConstructor.initialize() }
        )

        // 3. Use your common helper to apply the driver and build it
        getRoomDatabase(builder)
    }
}
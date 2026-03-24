package com.garrettbutchko.minimate.di
import com.garrettbutchko.minimate.room.AppDatabase
import com.garrettbutchko.minimate.room.getDatabaseBuilder
import com.garrettbutchko.minimate.room.getRoomDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<AppDatabase> {
        val builder = getDatabaseBuilder()
        getRoomDatabase(builder)
    }
}

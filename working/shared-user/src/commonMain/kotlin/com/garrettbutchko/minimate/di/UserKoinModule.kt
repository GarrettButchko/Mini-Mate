package com.garrettbutchko.minimate.repositories.Repositories.di

import org.koin.dsl.module

import com.garrettbutchko.minimate.repositories.UnifiedGameRepository

val sharedUserModule = module {
    // It can safely call get() for LocalGameRepository and RemoteGameRepository
    // as long as the core sharedModule is loaded alongside it.
    single { UnifiedGameRepository(local = get(), remote = get()) }
}
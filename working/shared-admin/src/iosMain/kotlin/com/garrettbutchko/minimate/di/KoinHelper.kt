package com.garrettbutchko.minimate.di

import com.garrettbutchko.minimate.viewModels.AuthViewModel
import com.garrettbutchko.minimate.viewModels.ProfileViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

// This object acts as a provider for Swift
object KoinHelper : KoinComponent {
    fun getAuthViewModel(): AuthViewModel = get()
    fun getProfileViewModel(): ProfileViewModel = get()
}
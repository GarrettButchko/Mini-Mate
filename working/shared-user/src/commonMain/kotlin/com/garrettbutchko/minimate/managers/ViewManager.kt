package com.garrettbutchko.minimate.managers

import com.garrettbutchko.minimate.interfaces.AppNavigationManaging
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ViewType {
    data class Main(val tab: Int) : ViewType()
    data object Welcome : ViewType()
    data class ScoreCard(val isGuest: Boolean = false) : ViewType()
    data class Ad(val isGuest: Boolean = false) : ViewType()
    data object SignIn : ViewType()
    data object Host : ViewType()
}

class ViewManager : AppNavigationManaging {

    private val _currentView = MutableStateFlow<ViewType>(ViewType.Welcome)
    val currentView: StateFlow<ViewType> = _currentView.asStateFlow()
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            _currentView.value = ViewType.Main(1)
        } else {
            scope.launch {
                try {
                    Firebase.auth.signOut()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            _currentView.value = ViewType.Welcome
        }
    }

    fun navigateToMain(tab: Int) {
        _currentView.value = ViewType.Main(tab)
    }

    fun navigateToSignIn() {
        _currentView.value = ViewType.SignIn
    }

    override fun navigateToWelcome() {
        _currentView.value = ViewType.Welcome
    }

    fun navigateToScoreCard(isGuest: Boolean = false) {
        _currentView.value = ViewType.ScoreCard(isGuest)
    }

    fun navigateToAd(isGuest: Boolean = false) {
        _currentView.value = ViewType.Ad(isGuest)
    }

    override fun navigateAfterSignIn() {
        navigateToMain(1)
    }

    fun navigateToHost() {
        _currentView.value = ViewType.Host
    }

    // Equivalent to the Swift Equatable extension that ignores associated values.
    // Useful if you need to check if the base type matches regardless of the parameters.
    fun isSameViewTypeBase(other: ViewType): Boolean {
        return this.currentView.value::class == other::class
    }
}

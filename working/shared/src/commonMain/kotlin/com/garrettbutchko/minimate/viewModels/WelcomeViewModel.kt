package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.interfaces.AppNavigationManaging
import com.garrettbutchko.minimate.utilities.NetworkChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class WelcomeViewModel(
    private val viewManager: AppNavigationManaging,
    private val welcomeText: String,
    private val networkChecker: NetworkChecker,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _displayedText = MutableStateFlow("")
    val displayedText: StateFlow<String> = _displayedText.asStateFlow()

    private val _showLoading = MutableStateFlow(false)
    val showLoading: StateFlow<Boolean> = _showLoading.asStateFlow()

    private val typingSpeedMs = 50L
    private var animationJob: Job? = null

    fun onAppear() {
        if (_displayedText.value.isEmpty()) {
            startTypingAnimation()
        }
    }
    
    fun onDisappear() {
        animationJob?.cancel()
        animationJob = null
    }

    private fun startTypingAnimation() {
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            var currentText = ""

            for (character in welcomeText) {
                currentText += character
                _displayedText.value = currentText
                delay(typingSpeedMs)
            }

            handleAnimationCompletion()
        }
    }

    private fun handleAnimationCompletion() {
        if (networkChecker.isConnected) {
            navigateToSignIn()
        } else {
            _showLoading.value = true
            pollUntilInternet()
        }
    }

    private fun pollUntilInternet() {
        animationJob?.cancel()
        animationJob = coroutineScope.launch {
            while (!networkChecker.isConnected) {
                delay(1000)
            }
            _showLoading.value = false
            navigateToSignIn()
        }
    }

    private fun navigateToSignIn() {
        viewManager.navigateToSignIn()
    }
}

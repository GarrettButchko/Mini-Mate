package com.garrettbutchko.minimate.viewModels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class JoinViewModel(
    val gameModel: GameViewModel,
    val authModel: AuthViewModel
) {
    // MARK: - UI State
    private val _gameCode = MutableStateFlow("")
    val gameCode: StateFlow<String> = _gameCode.asStateFlow()

    private val _inGame = MutableStateFlow(false)
    val inGame: StateFlow<Boolean> = _inGame.asStateFlow()

    private val _showExitAlert = MutableStateFlow(false)
    val showExitAlert: StateFlow<Boolean> = _showExitAlert.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    // MARK: - Actions

    fun setGameCode(code: String) {
        _gameCode.value = code
    }

    fun setShowExitAlert(show: Boolean) {
        _showExitAlert.value = show
    }

    fun joinGame() {
        val currentCode = _gameCode.value
        if (!canAttemptJoin(currentCode)) return

        val user = authModel.userModel.value ?: return
        
        gameModel.joinGame(id = currentCode, userId = user.googleId) { success, error ->
            if (success) {
                _inGame.value = true
            } else {
                error?.let {
                    _message.value = it
                }
            }
        }
    }

    fun leaveGame() {
        val user = authModel.userModel.value ?: return

        gameModel.leaveGame(userId = user.googleId)
        _gameCode.value = ""
        _inGame.value = false
    }

    // MARK: - External State Reactions

    fun hostDidDismiss(showHost: Boolean) {
        val game = gameModel.game.value
        if (shouldLeaveGameOnHostDismiss(showHost, game.id, game.started)) {
            gameModel.leaveGame(userId = game.id)
            _inGame.value = false
        }
    }

    fun gameDidStart(started: Boolean, onNavigate: () -> Unit) {
        if (shouldNavigateOnGameStart(started)) {
            onNavigate()
        }
    }

    fun gameDidDismiss(dismissed: Boolean) {
        if (shouldResetOnGameDismiss(dismissed)) {
            _gameCode.value = ""
            _inGame.value = false
        }
    }

    // MARK: - Internal Business Logic (Combined from JoinViewBusinessLogic)

    private fun canAttemptJoin(gameCode: String): Boolean {
        return gameCode.isNotBlank() && gameCode.length >= 6
    }

    private fun shouldLeaveGameOnHostDismiss(showHost: Boolean, gameId: String, hasStarted: Boolean): Boolean {
        return !showHost && gameId.isNotEmpty() && !hasStarted
    }

    private fun shouldNavigateOnGameStart(hasStarted: Boolean): Boolean {
        return hasStarted
    }

    private fun shouldResetOnGameDismiss(isDismissed: Boolean): Boolean {
        return isDismissed
    }
}

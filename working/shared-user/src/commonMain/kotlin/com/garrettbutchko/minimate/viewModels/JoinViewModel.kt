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
        println("JoinViewModel: joinGame called with code: $currentCode")
        
        if (!canAttemptJoin(currentCode)) {
            println("JoinViewModel: joinGame aborted - invalid code length")
            return
        }

        val user = authModel.userModel.value
        if (user == null) {
            println("JoinViewModel: joinGame aborted - no userModel found")
            return
        }
        
        println("JoinViewModel: Attempting to join game $currentCode for user ${user.googleId}")
        gameModel.joinGame(id = currentCode, userId = user.googleId) { success, error ->
            println("JoinViewModel: joinGame result - success: $success, error: $error")
            if (success) {
                _inGame.value = true
                _message.value = ""
            } else {
                error?.let {
                    _message.value = it
                }
            }
        }
    }

    fun leaveGame() {
        val user = authModel.userModel.value
        if (user == null) {
            println("JoinViewModel: leaveGame aborted - no userModel found")
            return
        }

        println("JoinViewModel: leaveGame called for user ${user.googleId}")
        gameModel.leaveGame(userId = user.googleId)
        _gameCode.value = ""
        _inGame.value = false
    }

    // MARK: - External State Reactions

    /**
     * Called when the Join/Lobby view is dismissed.
     */
    fun joinDidDismiss(showJoin: Boolean) {
        val game = gameModel.game.value
        val shouldLeave = shouldLeaveGameOnHostDismiss(showJoin, game.id, game.started)
        println("JoinViewModel: hostDidDismiss called - showHost: $showJoin, gameId: ${game.id}, started: ${game.started}, shouldLeave: $shouldLeave")
        
        if (shouldLeave) {
            println("JoinViewModel: shouldLeave is true, calling leaveGame()")
            leaveGame()
        }
    }

    /**
     * Called when the game's "started" status changes.
     */
    fun gameDidStart(started: Boolean, onNavigate: () -> Unit) {
        val shouldNav = shouldNavigateOnGameStart(started)
        println("JoinViewModel: gameDidStart reaction - started: $started, shouldNavigate: $shouldNav")
        
        if (shouldNav) {
            println("JoinViewModel: Triggering navigation callback")
            onNavigate()
        }
    }

    /**
     * Called when the game's "dismissed" status changes.
     */
    fun gameDidDismiss(dismissed: Boolean) {
        val shouldReset = shouldResetOnGameDismiss(dismissed)
        println("JoinViewModel: gameDidDismiss reaction - dismissed: $dismissed, shouldReset: $shouldReset")
        
        if (shouldReset) {
            println("JoinViewModel: Resetting game state")
            _gameCode.value = ""
            _inGame.value = false
        }
    }

    // MARK: - Internal Business Logic

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

package com.garrettbutchko.minimate.logic

/**
 * A platform-agnostic class designed to be easily migrated to a Kotlin Multiplatform shared module.
 * It contains pure business, validation, and presentation logic for joining a game.
 */
class JoinViewBusinessLogic {
    
    // MARK: - Input Formatting & Validation
    
    /**
     * Cleans and formats a scanned QR code string.
     */
    fun formatScannedCode(code: String): String {
        return code.trim().uppercase()
    }
    
    /**
     * Filters a manually typed game code to ensure it's valid (6 alphanumeric characters max).
     */
    fun formatEnteredCode(code: String): String {
        val filtered = code.uppercase().filter { it.isLetter() || it.isDigit() }
        return filtered.take(6)
    }
    
    // MARK: - Presentation Logic
    
    /**
     * Determines if the Join Game button should be disabled based on code validation.
     */
    fun isJoinButtonDisabled(gameCode: String): Boolean {
        return gameCode.length != 6
    }
    
    /**
     * Returns the appropriate opacity for the Join Game button.
     */
    fun getJoinButtonOpacity(gameCode: String): Double {
        return if (isJoinButtonDisabled(gameCode)) 0.5 else 1.0
    }
    
    /**
     * Provides a fallback string for a missing location name.
     */
    fun getLocationName(name: String?): String {
        return name ?: "No Location"
    }
    
    /**
     * Formats the players section header.
     */
    fun getPlayersHeaderText(playerCount: Int): String {
        return "Players: $playerCount"
    }
    
    // MARK: - ViewModel State Logic
    
    /**
     * Validates if a join attempt should proceed based on the game code.
     */
    fun canAttemptJoin(gameCode: String): Boolean {
        return gameCode.isNotEmpty()
    }
    
    /**
     * Determines if a joined player should be removed when the view's host state changes.
     */
    fun shouldLeaveGameOnHostDismiss(showHost: Boolean, gameId: String, hasStarted: Boolean): Boolean {
        return !showHost && gameId.isNotEmpty() && !hasStarted
    }
    
    /**
     * Validates if navigation should occur based on the game's started status.
     */
    fun shouldNavigateOnGameStart(hasStarted: Boolean): Boolean {
        return hasStarted
    }
    
    /**
     * Validates if the local state should be reset because the game was dismissed.
     */
    fun shouldResetOnGameDismiss(isDismissed: Boolean): Boolean {
        return isDismissed
    }
}

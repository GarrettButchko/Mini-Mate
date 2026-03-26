package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.dataModels.mapModels.MapItemDTO
import com.garrettbutchko.minimate.managers.ViewManager
import com.garrettbutchko.minimate.repositories.CourseRepository
import com.garrettbutchko.minimate.utilities.generateQRCodeData
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class HostViewModel(
    private val courseRepo: CourseRepository,
    val gameModel: GameViewModel,
    val viewManager: ViewManager,
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val ttl: Double = 5 * 60.0
    private var lastUpdated: Instant = Clock.System.now()
    private var lastResetTime: Instant? = null
    private val resetCooldown: Double = 2.0

    private val _timeRemaining = MutableStateFlow(ttl)
    val timeRemaining: StateFlow<Double> = _timeRemaining.asStateFlow()

    private val _playerToDelete = MutableStateFlow<String?>(null)
    val playerToDelete: StateFlow<String?> = _playerToDelete.asStateFlow()

    private val _showTextAndButtons = MutableStateFlow(false)
    val showTextAndButtons: StateFlow<Boolean> = _showTextAndButtons.asStateFlow()

    private val _isRotating = MutableStateFlow(false)
    val isRotating: StateFlow<Boolean> = _isRotating.asStateFlow()

    private val _showLocationButton = MutableStateFlow(false)
    val showLocationButton: StateFlow<Boolean> = _showLocationButton.asStateFlow()

    private val _qrCodeImage = MutableStateFlow<ByteArray?>(null)
    val qrCodeImage: StateFlow<ByteArray?> = _qrCodeImage.asStateFlow()

    private val _showQRCode = MutableStateFlow(false)
    val showQRCode: StateFlow<Boolean> = _showQRCode.asStateFlow()

    private val _showAddLocalPlayer = MutableStateFlow(false)
    val showAddLocalPlayer: StateFlow<Boolean> = _showAddLocalPlayer.asStateFlow()

    private val _showDeleteAlert = MutableStateFlow(false)
    val showDeleteAlert: StateFlow<Boolean> = _showDeleteAlert.asStateFlow()

    private var timerJob: Job? = null

    init {
        _timeRemaining.value = ttl
    }

    // MARK: - Presentation Logic
    
    fun getHeaderTitle(isOnline: Boolean): String {
        return if (isOnline) "Hosting Game" else "Game Setup"
    }
    
    fun getPlayersHeaderText(playerCount: Int): String {
        return "Players: $playerCount"
    }
    
    // MARK: - View Logic
    
    /**
     * Determines if the game should be dismissed when the view disappears.
     */
    fun shouldDismissGameOnDisappear(isStarted: Boolean, isDismissed: Boolean, showHost: Boolean): Boolean {
        return !isStarted && !isDismissed && !showHost
    }
    
    /**
     * Orchestrates the actions required when a guest clicks the back button.
     */
    fun handleGuestBackAction(dismissGame: () -> Unit, navigateToSignIn: () -> Unit) {
        navigateToSignIn()
        dismissGame()
    }
    
    /**
     * Orchestrates the process of starting a game, conditionally handling guest storage.
     */
    fun handleStartGame(isGuest: Boolean, deleteGuestGame: () -> Unit, performStart: () -> Unit) {
        performStart()
        if (isGuest) {
            deleteGuestGame()
        }
    }
    
    /**
     * Evaluates user deletion and delegates the removal if valid.
     */
    fun handleDeletePlayer(playerId: String?, removePlayer: (String) -> Unit) {
        playerId?.let {
            removePlayer(it)
        }
    }

    // MARK: - ViewModel Logic

    fun startTimer(onTimeout: (Boolean) -> Unit) {
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (true) {
                tick(onTimeout)
                delay(1.seconds)
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun tick(onTimeout: (Boolean) -> Unit) {
        if (!gameModel.onlineGame.value) return

        val currentTime = Clock.System.now()
        val remaining = calculateTimeRemaining(lastUpdated, ttl, currentTime)
        _timeRemaining.value = remaining

        if (remaining <= 0.0) {
            stopTimer()
            gameModel.dismissGame()
            onTimeout(false)
        }
    }

    fun resetTimer() {
        if (!gameModel.onlineGame.value) return

        val currentTime = Clock.System.now()
        if (!canResetTimer(lastResetTime, resetCooldown, currentTime)) {
            return
        }

        lastUpdated = currentTime
        lastResetTime = currentTime
        gameModel.setLastUpdated(Timestamp(currentTime.epochSeconds, 0))
        _timeRemaining.value = ttl
    }

    fun addPlayer(newPlayerName: String, newPlayerEmail: String, playerBallColor: String? = null) {
        gameModel.addLocalPlayer(newPlayerName, newPlayerEmail, playerBallColor)
        resetTimer()
    }

    fun deletePlayer() {
        _playerToDelete.value?.let { id ->
            gameModel.removePlayer(id)
            resetTimer()
        }
    }

    fun startGame(onHostHidden: () -> Unit, isGuest: Boolean = false) {
        gameModel.startGame {
            onHostHidden()
        }
        viewManager.navigateToScoreCard(isGuest)
    }

    fun handleLocationChange(item: MapItemDTO?) {
        val name = item?.name ?: return
        
        coroutineScope.launch {
            if (courseRepo.courseNameExistsAndSupported(name)) {
                val course = courseRepo.fetchCourseByName(name)
                val holes = determineDefaultHoles(course?.pars?.size)
                gameModel.setNumberOfHole(holes)
            }
            resetTimer()
        }
    }

    fun searchNearby() {
        gameModel.setHasLoaded(false)
        coroutineScope.launch {
            gameModel.findClosestLocationAndLoadCourse()
            resetTimer()
        }
    }

    fun retry() {
        _isRotating.value = true
        searchNearby()
        coroutineScope.launch {
            delay(1000)
            _isRotating.value = false
            resetTimer()
        }
    }

    fun exit() {
        gameModel.resetCourse()
        _showTextAndButtons.value = false
    }

    fun setUp() {
        _qrCodeImage.value = generateQRCodeData(gameModel.game.value.id)
        coroutineScope.launch {
            courseFind()
        }
    }

    suspend fun courseFind() {
        if (!_showLocationButton.value) return
        
        if (gameModel.course.value == null && !gameModel.hasLoaded) {
            gameModel.findClosestLocationAndLoadCourse()
            gameModel.setHasLoaded(true)
        }
    }

    fun timeString(): String {
        return formatTimeString(_timeRemaining.value.toInt())
    }

    fun setQRCodeImage(data: ByteArray?) {
        _qrCodeImage.value = data
    }

    // MARK: - Internal Logic (Combined from HostViewBusinessLogic)

    private fun calculateTimeRemaining(lastUpdated: Instant, ttlSeconds: Double, currentTime: Instant): Double {
        val expire = lastUpdated.plus(ttlSeconds.seconds)
        val remaining = expire - currentTime
        return if (remaining.isPositive()) remaining.toDouble(DurationUnit.SECONDS) else 0.0
    }
    
    private fun canResetTimer(lastReset: Instant?, cooldownSeconds: Double, currentTime: Instant): Boolean {
        if (lastReset == null) return true
        return (currentTime - lastReset) >= cooldownSeconds.seconds
    }
    
    private fun formatTimeString(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        val secsStr = if (secs < 10) "0$secs" else "$secs"
        return "$minutes:$secsStr"
    }
    
    private fun determineDefaultHoles(parsCount: Int?): Int {
        return parsCount ?: 18
    }

    // Setters for Swift interop if needed or common usage
    fun setPlayerToDelete(id: String?) { _playerToDelete.value = id }
    fun setShowTextAndButtons(value: Boolean) { _showTextAndButtons.value = value }
    fun setIsRotating(value: Boolean) { _isRotating.value = value }
    fun setShowLocationButton(value: Boolean) { _showLocationButton.value = value }
    fun setShowQRCode(value: Boolean) { _showQRCode.value = value }
    fun setShowAddLocalPlayer(value: Boolean) { _showAddLocalPlayer.value = value }
    fun setShowDeleteAlert(value: Boolean) { _showDeleteAlert.value = value }
}

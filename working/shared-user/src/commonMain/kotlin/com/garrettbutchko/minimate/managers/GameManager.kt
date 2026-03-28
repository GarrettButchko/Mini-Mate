package com.garrettbutchko.minimate.managers

import co.touchlab.kermit.Logger
import com.garrettbutchko.minimate.dataModels.gameModels.Game
import com.garrettbutchko.minimate.dataModels.holeModels.Hole
import com.garrettbutchko.minimate.dataModels.playerModels.Player
import com.garrettbutchko.minimate.datamodels.UserModel
import com.garrettbutchko.minimate.extensions.toInstant
import com.garrettbutchko.minimate.repositories.gameRepos.LocalGameRepository
import com.garrettbutchko.minimate.repositories.gameRepos.RemoteGameRepository
import com.garrettbutchko.minimate.utilities.NetworkChecker
import com.garrettbutchko.minimate.viewModels.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.round

class GameManager(
    private val authModel: AuthViewModel,
    private val localGameRepo: LocalGameRepository,
    private val remoteGameRepo: RemoteGameRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val log = Logger.withTag("GameManager")

    private val _userGames = MutableStateFlow<List<Game>>(emptyList())
    val userGames: StateFlow<List<Game>> = _userGames.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Fixed: Use a computed property so it always reflects the current user's ID
    val userID: String? get() = authModel.userModel.value?.googleId

    init {
        // Automatically keep userGames in sync whenever the user's gameIDs change
        // This handles the case where the user model takes time to load (it will emit when ready)
        coroutineScope.launch {
            authModel.userModel.collectLatest { user ->
                if (user != null) {
                    log.d { "👤 User changed/loaded: ${user.name}. Initializing games sync..." }
                    syncGames(user)
                } else {
                    log.d { "👤 User logged out. Clearing games." }
                    _userGames.value = emptyList()
                }
            }
        }
    }

    /**
     * Manual trigger for UI onAppear calls.
     */
    fun onAppear() {
        val user = authModel.userModel.value
        log.d { "📊 onAppear: user=${user?.name}, currentGamesCount=${userGames.value.size}" }

        // Trigger a cloud refresh if needed
        if (user != null && NetworkChecker.shared.isConnected && userGames.value.size != user.gameIDs.size) {
            coroutineScope.launch {
                refreshFromCloud(user)
            }
        }
    }

    /**
     * Orchestrates the local fetch and background cloud refresh.
     */
    private suspend fun syncGames(user: UserModel) {
        // 1. Initial local fetch to populate UI quickly
        val games = localGameRepo.fetchAll(ids = user.gameIDs)
        _userGames.value = games
        log.d { "📦 Initial local fetch returned ${games.size}/${user.gameIDs.size} games" }

        // 2. If there are missing games locally and we have internet, trigger a background refresh
        if (NetworkChecker.shared.isConnected && games.size != user.gameIDs.size) {
            refreshFromCloud(user)
        }
    }

    /**
     * Fetches missing games from the cloud and updates the local state.
     */
    suspend fun refreshFromCloud(user: UserModel) {
        if (_isRefreshing.value) return

        log.d { "☁️ Starting cloud refresh for missing games..." }
        _isRefreshing.value = true

        try {
            val missingIDs = localGameRepo.getMissingLocalGameIDs(user.gameIDs)
            if (missingIDs.isEmpty()) {
                log.d { "✅ No missing games locally." }
                return
            }

            log.d { "📡 Fetching ${missingIDs.size} games from Remote..." }
            val remoteDTOs = remoteGameRepo.fetchAll(missingIDs)
            val remoteGames = remoteDTOs.map { it.toGame() }

            if (remoteGames.isNotEmpty()) {
                val success = localGameRepo.save(remoteGames)
                if (success) {
                    log.d { "✅ Saved ${remoteGames.size} games to local DB" }
                    // Re-fetch all to ensure full consistency and update the StateFlow
                    _userGames.value = localGameRepo.fetchAll(ids = user.gameIDs)
                }
            }
        } catch (e: Exception) {
            log.e(e) { "❌ Error during cloud refresh" }
        } finally {
            _isRefreshing.value = false
        }
    }

    // MARK: - Utility for identifying "the user" in a game

    private fun findUserInGame(game: Game): Player? {
        val currentUserID = userID
        // 1. Prefer matching by the exact current userID
        if (currentUserID != null) {
            val player = game.players.firstOrNull { it.userId == currentUserID }
            if (player != null) return player
        }
        
        // 2. Fallback to hostUserId (in case they played guest games that got linked)
        val hostPlayer = game.players.firstOrNull { it.userId == game.hostUserId }
        if (hostPlayer != null) return hostPlayer
        
        // 3. Last resort: first player (usually the one who started the game)
        return game.players.firstOrNull()
    }

    // MARK: - General Stats

    fun totalGamesPlayed(): Int {
        return _userGames.value.size
    }

    fun totalPlayersFaced(): Int {
        val allOpponentIDs = _userGames.value.flatMap { game ->
            val userPlayer = findUserInGame(game)
            game.players.filter { it.id != userPlayer?.id }.map { it.userId }
        }.toSet()
        return allOpponentIDs.size
    }

    fun totalStrokes(): Int {
        return _userGames.value
            .mapNotNull { findUserInGame(it) }
            .flatMap { it.holes }
            .sumOf { it.strokes }
    }

    fun totalHolesPlayed(): Int {
        return _userGames.value
            .mapNotNull { findUserInGame(it) }
            .flatMap { it.holes }
            .size
    }

    fun averageStrokesPerGame(): Double {
        val gamesCount = totalGamesPlayed()
        if (gamesCount == 0) return 0.0
        return totalStrokes().toDouble() / gamesCount
    }

    fun averageStrokesPerHole(): Double {
        val holesCount = totalHolesPlayed()
        if (holesCount == 0) return 0.0
        return totalStrokes().toDouble() / holesCount
    }

    // MARK: - Performance Stats

    fun bestGameStrokes(): Int? {
        return _userGames.value.mapNotNull { game ->
            findUserInGame(game)?.totalStrokes
        }.minOrNull()
    }

    fun worstGameStrokes(): Int? {
        return _userGames.value.mapNotNull { game ->
            findUserInGame(game)?.totalStrokes
        }.maxOrNull()
    }

    fun holeInOneCount(): Int {
        return _userGames.value
            .mapNotNull { findUserInGame(it) }
            .flatMap { it.holes }
            .count { it.strokes == 1 }
    }

    // MARK: - Average Holes Maps

    fun averageHoles9(): List<Hole> {
        return averageHolesMap(9)
    }

    fun averageHoles18(): List<Hole> {
        return averageHolesMap(18)
    }

    private fun averageHolesMap(numberOfHoles: Int): List<Hole> {
        val holeStrokesDict = mutableMapOf<Int, MutableList<Int>>()

        for (game in _userGames.value) {
            val player = findUserInGame(game)
            if (player != null) {
                for (hole in player.holes) {
                    if (hole.number <= numberOfHoles) {
                        holeStrokesDict.getOrPut(hole.number) { mutableListOf() }.add(hole.strokes)
                    }
                }
            }
        }

        val averageHoles = mutableListOf<Hole>()
        for (holeNumber in 1..numberOfHoles) {
            val strokesList = holeStrokesDict[holeNumber] ?: emptyList()
            val average = if (strokesList.isEmpty()) 0.0 else strokesList.sum().toDouble() / strokesList.size

            // Following the Swift behavior of rounding the average value.
            // Since Hole takes an Int for strokes, we round it.
            val roundedAverage = round(average).toInt()
            averageHoles.add(Hole(number = holeNumber, strokes = roundedAverage))
        }

        return averageHoles
    }

    // MARK: - Latest Game Stats

    fun latestGame(): Game? {
        return _userGames.value.maxByOrNull { it.date.toInstant() }
    }

    fun winnerOfLatestGame(): Player? {
        return latestGame()?.players?.minByOrNull { it.totalStrokes }
    }

    fun usersScoreOfLatestGame(): Int {
        return latestGame()?.let { findUserInGame(it) }?.totalStrokes ?: 0
    }

    fun usersHolesOfLatestGame(): List<Hole> {
        return latestGame()?.let { findUserInGame(it) }?.holes ?: emptyList()
    }

    fun hasGames(): Boolean {
        return _userGames.value.isNotEmpty()
    }
}

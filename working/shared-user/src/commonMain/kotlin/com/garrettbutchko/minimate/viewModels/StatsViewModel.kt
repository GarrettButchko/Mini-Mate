package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.analyzers.UserStatsAnalyzer
import com.garrettbutchko.minimate.dataModels.gameModels.Game
import com.garrettbutchko.minimate.datamodels.UserModel
import com.garrettbutchko.minimate.repositories.gameRepos.LocalGameRepository
import com.garrettbutchko.minimate.repositories.gameRepos.RemoteGameRepository
import com.garrettbutchko.minimate.utilities.NetworkChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import co.touchlab.kermit.Logger

class StatsViewModel(
    private val localGameRepo: LocalGameRepository,
    private val remoteGameRepo: RemoteGameRepository,
    private val authModel: AuthViewModel,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val log = Logger.withTag("StatsViewModel")

    // MARK: - UI State
    private val _pickedSection = MutableStateFlow("Games")
    val pickedSection: StateFlow<String> = _pickedSection.asStateFlow()

    val pickerSections: List<String> = listOf("Games", "Overview")

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _latest = MutableStateFlow(true)
    val latest: StateFlow<Boolean> = _latest.asStateFlow()

    private val _editOn = MutableStateFlow(false)
    val editOn: StateFlow<Boolean> = _editOn.asStateFlow()

    private val _editingGameID = MutableStateFlow<String?>(null)
    val editingGameID: StateFlow<String?> = _editingGameID.asStateFlow()

    private val _isSharePresented = MutableStateFlow(false)
    val isSharePresented: StateFlow<Boolean> = _isSharePresented.asStateFlow()

    private val _shareContent = MutableStateFlow("")
    val shareContent: StateFlow<String> = _shareContent.asStateFlow()

    private val _isCooldown = MutableStateFlow(false)
    val isCooldown: StateFlow<Boolean> = _isCooldown.asStateFlow()

    private val _isCooldown2 = MutableStateFlow(false)
    val isCooldown2: StateFlow<Boolean> = _isCooldown2.asStateFlow()

    // MARK: - Derived / Computed
    private val _analyzer = MutableStateFlow<UserStatsAnalyzer?>(null)
    val analyzer: StateFlow<UserStatsAnalyzer?> = _analyzer.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _allGames = MutableStateFlow<List<Game>>(emptyList())
    val allGames: StateFlow<List<Game>> = _allGames.asStateFlow()

    // MARK: - Actions

    fun setPickedSection(section: String) {
        _pickedSection.value = section
    }

    fun setSearchText(text: String) {
        _searchText.value = text
    }

    fun setLatest(value: Boolean) {
        _latest.value = value
    }

    fun setEditOn(value: Boolean) {
        _editOn.value = value
    }

    fun setEditingGameID(id: String?) {
        _editingGameID.value = id
    }

    fun setIsSharePresented(value: Boolean) {
        _isSharePresented.value = value
    }

    suspend fun onAppear(): List<Game> {
        val user = authModel.userModel.value
        val gameIds = user?.gameIDs ?: emptyList()
        
        log.d { "📊 onAppear: user=${user?.name}, gameIDsCount=${gameIds.size}" }

        // 1. Initial local fetch
        val games = localGameRepo.fetchAll(ids = gameIds)
        _allGames.value = games
        log.d { "📦 Initial local fetch returned ${games.size} games" }

        // 2. Update analyzer with what we have
        if (user != null) {
            _analyzer.value = UserStatsAnalyzer(userModel = user, games = games)
        }
        
        // 3. If there are missing games locally, trigger a background refresh
        if (user != null && NetworkChecker.shared.isConnected) {
            coroutineScope.launch {
                refreshFromCloudIfNeeded(user) {
                    // Update the list again after refresh
                    coroutineScope.launch {
                        val updatedGames = localGameRepo.fetchAll(ids = user.gameIDs)
                        _allGames.value = updatedGames
                        _analyzer.value = UserStatsAnalyzer(userModel = user, games = updatedGames)
                        log.d { "🔄 List updated after cloud refresh: ${updatedGames.size} games" }
                    }
                }
            }
        }
        
        return games
    }

    fun toggleSortWithCooldown() {
        if (_isCooldown.value) return

        _editingGameID.value = null
        _latest.value = !_latest.value

        _isCooldown.value = true
        coroutineScope.launch {
            delay(1000)
            _isCooldown.value = false
        }
    }

    fun presentShareSheet(text: String) {
        _shareContent.value = text
        _isSharePresented.value = true
    }

    fun refreshFromCloudIfNeeded(
        user: UserModel,
        completion: () -> Unit
    ) {
        if (_isCooldown2.value) {
            log.d { "⏳ Cloud refresh on cooldown" }
            completion()
            return
        }

        if (!NetworkChecker.shared.isConnected) {
            log.d { "🌐 No internet, skipping cloud refresh" }
            coolDown2()
            completion()
            return
        }

        log.d { "☁️ Starting cloud refresh for ${user.gameIDs.size} game IDs" }
        _isRefreshing.value = true
        
        coroutineScope.launch {
            try {
                val missingIDs = localGameRepo.getMissingLocalGameIDs(user.gameIDs)
                log.d { "🔍 Missing local games count: ${missingIDs.size}" }
                
                if (missingIDs.isEmpty()) {
                    log.d { "✅ No missing games locally" }
                    _isRefreshing.value = false
                    coolDown2()
                    completion()
                    return@launch
                }

                log.d { "📡 Fetching ${missingIDs.size} games from Firestore..." }
                val remoteDTOs = remoteGameRepo.fetchAll(missingIDs)
                log.d { "📥 Firestore returned ${remoteDTOs.size} games" }
                
                val remoteGames = remoteDTOs.map { it.toGame() }

                if (remoteGames.isNotEmpty()) {
                    val success = localGameRepo.save(remoteGames)
                    if (success) {
                        log.d { "✅ Saved ${remoteGames.size} games to local DB" }
                    } else {
                        log.e { "❌ Failed to save games to local DB" }
                    }
                }
            } catch (e: Exception) {
                log.e(e) { "❌ Error refreshing from cloud" }
            } finally {
                _isRefreshing.value = false
                coolDown2()
                completion()
            }
        }
    }

    private fun coolDown2() {
        _isCooldown2.value = true
        coroutineScope.launch {
            delay(1000)
            _isCooldown2.value = false
        }
    }
}

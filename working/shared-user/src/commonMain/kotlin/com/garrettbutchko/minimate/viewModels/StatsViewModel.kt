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

class StatsViewModel(
    private val localGameRepo: LocalGameRepository,
    private val remoteGameRepo: RemoteGameRepository,
    private val authModel: AuthViewModel,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
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

    fun onAppear(games: List<Game>) {
        authModel.userModel.let {
            if (it.value != null){
                _analyzer.value = UserStatsAnalyzer(userModel = it.value!!, games = games)
            }
        }
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
        if (_isCooldown2.value) return

        if (!NetworkChecker.shared.isConnected) {
            coolDown2()
            completion()
            return
        }

        _isRefreshing.value = true
        
        coroutineScope.launch {
            val missingIDs = localGameRepo.getMissingLocalGameIDs(user.gameIDs)
            
            if (missingIDs.isEmpty()) {
                _isRefreshing.value = false
                coolDown2()
                completion()
                return@launch
            }

            val remoteDTOs = remoteGameRepo.fetchAll(missingIDs)
            val remoteGames = remoteDTOs.map { it.toGame() }

            val success = localGameRepo.save(remoteGames)
            if (success) {
                println("✅ Refreshed ${remoteGames.size} missing games from cloud")
            } else {
                println("❌ Failed saving refreshed games locally")
            }

            _isRefreshing.value = false
            coolDown2()
            completion()
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

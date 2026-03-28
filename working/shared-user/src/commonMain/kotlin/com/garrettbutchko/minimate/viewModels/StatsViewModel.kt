package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.dataModels.gameModels.Game
import com.garrettbutchko.minimate.repositories.gameRepos.LocalGameRepository
import com.garrettbutchko.minimate.repositories.gameRepos.RemoteGameRepository
import com.garrettbutchko.minimate.managers.GameManager
import com.garrettbutchko.minimate.utilities.NetworkChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import co.touchlab.kermit.Logger
import dev.gitlive.firebase.firestore.Timestamp

class StatsViewModel(
    private val localGameRepo: LocalGameRepository,
    private val remoteGameRepo: RemoteGameRepository,
    private val authModel: AuthViewModel,
    private val gameManager: GameManager,
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

    val isRefreshing: StateFlow<Boolean> = gameManager.isRefreshing
    val allGames: StateFlow<List<Game>> = gameManager.userGames

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

    fun onAppear() {
        val user = authModel.userModel.value

        // Trigger a cloud refresh if needed
        if (user != null && NetworkChecker.shared.isConnected && allGames.value.size != user.gameIDs.size) {
            coroutineScope.launch {
                gameManager.refreshFromCloud(user)
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

    fun deleteGame(gameID: String) {
        val user = authModel.userModel.value ?: return

        coroutineScope.launch {
            try {
                // 1. Update the user model to remove the game ID
                val updatedGameIDs = user.gameIDs.filter { it != gameID }
                val updatedUser = user.copy(gameIDs = updatedGameIDs, lastUpdated = Timestamp.now())

                // Save updated user (both Local and Remote)
                authModel.userRepository.saveUnified(updatedUser.googleId, updatedUser)

                // Update the state in AuthViewModel so it propagates to GameManager and our analyzer
                authModel.setUserModel(updatedUser)

                // 2. Delete the game itself from repositories
                localGameRepo.delete(gameID)
                remoteGameRepo.delete(gameID)

                log.d { "🗑️ Game $gameID removed from user and deleted from repositories" }
            } catch (e: Exception) {
                log.e(e) { "❌ Failed to complete game deletion for $gameID" }
            }
        }
    }
}

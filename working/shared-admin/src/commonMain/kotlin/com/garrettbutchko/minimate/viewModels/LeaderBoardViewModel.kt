package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.dataModels.playerModels.LeaderboardEntry
import com.garrettbutchko.minimate.repositories.CourseLeaderboardRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class LeaderBoardViewModel(
    val lbRepo: CourseLeaderboardRepository = CourseLeaderboardRepository(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _allTimeLeaderboard = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val allTimeLeaderboard: StateFlow<List<LeaderboardEntry>> = _allTimeLeaderboard.asStateFlow()

    private var listenJob: Job? = null

    // MARK: - Lifecycle

    fun setAllTimeLeaderboard(leaderboard: List<LeaderboardEntry>) {
        _allTimeLeaderboard.value = leaderboard
    }

    fun onAppear(courseID: String) {
        listenJob?.cancel()
        listenJob = coroutineScope.launch {
            lbRepo.listenTopAllTime(courseID).collect { entries ->
                _allTimeLeaderboard.value = entries
            }
        }
    }

    // ALWAYS do this to prevent memory leaks and unnecessary Firebase costs
    fun onDisappear() {
        listenJob?.cancel()
        listenJob = null
    }

    // MARK: - Actions
    
    fun deletePlayerEntry(courseID: String, playerID: String) {
        coroutineScope.launch {
            lbRepo.deleteEntry(courseID, playerID)
            
            // Optimistic UI update
            val currentList = _allTimeLeaderboard.value.toMutableList()
            currentList.removeAll { it.id == playerID }
            _allTimeLeaderboard.value = currentList
        }
    }
}

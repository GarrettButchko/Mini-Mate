package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.dataModels.mapModels.MapItemDTO
import com.garrettbutchko.minimate.dataModels.courseModels.Course
import com.garrettbutchko.minimate.dataModels.gameModels.Game
import com.garrettbutchko.minimate.dataModels.holeModels.Hole
import com.garrettbutchko.minimate.dataModels.playerModels.Player
import com.garrettbutchko.minimate.dataModels.playerModels.PlayerDTO
import com.garrettbutchko.minimate.interfaces.LocationFinding
import com.garrettbutchko.minimate.repositories.AnalyticsRepository
import com.garrettbutchko.minimate.repositories.CourseRepository
import com.garrettbutchko.minimate.repositories.LiveGameRepository
import com.garrettbutchko.minimate.repositories.UnifiedGameRepository
import com.garrettbutchko.minimate.repositories.gameRepos.LocalGameRepository
import com.garrettbutchko.minimate.repositories.userRepos.RemoteUserRepository
import com.garrettbutchko.minimate.utilities.CourseIDGenerator
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.ChildEvent
import dev.gitlive.firebase.database.database
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.compose.runtime.MutableState
import com.garrettbutchko.minimate.dataModels.gameModels.GameDTO

data class GuestData(
    val id: String,
    val email: String? = null,
    val name: String,
    val ballColorDT: String? = null
)

sealed class JoinGameStatus {
    object Success : JoinGameStatus()
    data class Error(val message: String) : JoinGameStatus()
}

class GameViewModel(
    val authModel: AuthViewModel,
    val liveGameRepo: LiveGameRepository,
    val unifiedGameRepository: UnifiedGameRepository,
    val localGameRepository: LocalGameRepository,
    val courseRepo: CourseRepository,
    val analyticsRepo: AnalyticsRepository,
    val remoteUserRepo: RemoteUserRepository,
    val locationHandler: LocationFinding,
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    initialGame: Game = Game(
        id = "",
        date = Timestamp.now(),
        completed = false,
        numberOfHoles = 18,
        started = false,
        dismissed = false,
        live = false,
        lastUpdated = Timestamp.now(),
        players = emptyList<Player>(),
    ),
    initialCourse: Course? = null,
    initialOnlineGame: Boolean = true
) {
    private val _game = MutableStateFlow(initialGame)
    val game: StateFlow<Game> = _game.asStateFlow()

    private val _course = MutableStateFlow(initialCourse)
    var course: StateFlow<Course?> = _course.asStateFlow()

    private val _onlineGame = MutableStateFlow(initialOnlineGame)
    val onlineGame: StateFlow<Boolean> = _onlineGame.asStateFlow()

    private var lastUpdated: Timestamp = Timestamp.now()
    var hasLoaded: Boolean = false
    private var isDismissing: Boolean = false
    private var listenerJob: Job? = null


    fun setHasLoaded(loaded: Boolean) {
        hasLoaded = loaded
    }

    fun setCourse(newCourse: Course?) {
        _course.value = newCourse
    }

    fun setOnlineGame(value: Boolean) {
        _onlineGame.value = value
    }

    fun setIsDismissing(value: Boolean) {
        isDismissing = value
    }

    fun setListenerJob(job: Job?) {
        listenerJob = job
    }

    fun resetGame() {
        setGame(Game(), listen = false)
    }

    fun setGame(newGame: Game, listen: Boolean = true) {
        stopListening()
        
        val playersWithHoles = newGame.players.map { remotePlayer ->
            initializeHolesForPlayer(remotePlayer, newGame.numberOfHoles)
        }
        
        val mergedGame = newGame.copy(
            players = playersWithHoles,
            lastUpdated = newGame.lastUpdated
        )
        
        lastUpdated = mergedGame.lastUpdated
        _game.value = mergedGame
        
        if (listen && _onlineGame.value && !isDismissing) {
            listenForUpdates()
        }
    }

    fun setCompletedGame(completedGame: Boolean) {
        lastUpdated = Timestamp.now()
        _game.value = _game.value.copy(
            completed = completedGame,
            lastUpdated = lastUpdated
        )
        pushUpdate()
    }

    fun setNumberOfHole(holes: Int) {
        lastUpdated = Timestamp.now()
        _game.value = _game.value.copy(
            numberOfHoles = holes,
            lastUpdated = lastUpdated
        )
        pushUpdate()
    }

    fun stopListening() {
        listenerJob?.cancel()
        listenerJob = null
    }

    fun setLastUpdated(date: Timestamp) {
        lastUpdated = date
        _game.value = _game.value.copy(lastUpdated = lastUpdated)
        pushUpdate()
    }

    fun pushUpdate() {
        if (isDismissing) return
        val currentId = _game.value.id
        if (currentId.isEmpty() || currentId.any { it in ".#$[]" }) return

        lastUpdated = Timestamp.now()

        // Create a new list instance to ensure SwiftUI/StateFlow observers detect the change
        val updatedPlayers = _game.value.players.toList()

        _game.value = _game.value.copy(
            lastUpdated = lastUpdated,
            players = updatedPlayers
        )

        if (!_onlineGame.value) return
        liveGameRepo.addOrUpdateGame(_game.value) { _ -> }
    }

    fun listenForUpdates() {
        val currentId = _game.value.id
        if (!_onlineGame.value || currentId.isEmpty() || currentId.any { it in ".#$[]" }) return

        val ref = Firebase.database.reference("live_games").child(currentId)

        listenerJob?.cancel()
        listenerJob = coroutineScope.launch {

            // 1. Listen for Metadata Changes
            launch {
                ref.childEvents().collect { event ->
                    // Catch initial ADDED and subsequent CHANGED events
                    if (event.type != ChildEvent.Type.CHANGED && event.type != ChildEvent.Type.ADDED) return@collect

                    val key = event.snapshot.key ?: return@collect
                    if (key == "players") return@collect

                    val rawValue = event.snapshot.value
                    
                    _game.update { currentGame ->
                        when (key) {
                            "id" -> currentGame.copy(id = rawValue as? String ?: currentGame.id)
                            "hostUserId" -> currentGame.copy(hostUserId = rawValue as? String ?: currentGame.hostUserId)
                            "date" -> {
                                val tsMillis = (rawValue as? Number)?.toLong()
                                val ts = if (tsMillis != null) Timestamp(tsMillis / 1000, ((tsMillis % 1000) * 1000000).toInt()) else null
                                if (ts != null) currentGame.copy(date = ts) else currentGame
                            }
                            "numberOfHoles" -> {
                                val num = (rawValue as? Number)?.toInt()
                                currentGame.copy(numberOfHoles = num ?: currentGame.numberOfHoles)
                            }

                            "live" -> currentGame.copy(live = rawValue as? Boolean ?: currentGame.live)
                            "lastUpdated" -> {
                                val tsMillis = (rawValue as? Number)?.toLong()
                                val ts = if (tsMillis != null) Timestamp(tsMillis / 1000, ((tsMillis % 1000) * 1000000).toInt()) else null
                                if (ts != null) currentGame.copy(lastUpdated = ts) else currentGame
                            }
                            "courseID" -> currentGame.copy(courseID = rawValue as? String)
                            "locationName" -> currentGame.copy(locationName = rawValue as? String)
                            "startTime" -> {
                                val tsMillis = (rawValue as? Number)?.toLong()
                                val ts = if (tsMillis != null) Timestamp(tsMillis / 1000, ((tsMillis % 1000) * 1000000).toInt()) else null
                                if (ts != null) currentGame.copy(startTime = ts) else currentGame
                            }
                            "endTime" -> {
                                val tsMillis = (rawValue as? Number)?.toLong()
                                val ts = if (tsMillis != null) Timestamp(tsMillis / 1000, ((tsMillis % 1000) * 1000000).toInt()) else null
                                if (ts != null) currentGame.copy(endTime = ts) else currentGame
                            }
                            "started" -> {
                                val value = rawValue as? Boolean ?: ((rawValue as? Number)?.toInt() == 1)
                                println("GameViewModel: Metadata update - started: $value")
                                currentGame.copy(started = value)
                            }
                            "dismissed" -> {
                                val value = rawValue as? Boolean ?: ((rawValue as? Number)?.toInt() == 1)
                                println("GameViewModel: Metadata update - dismissed: $value")
                                currentGame.copy(dismissed = value)
                            }
                            "completed" -> {
                                val value = rawValue as? Boolean ?: ((rawValue as? Number)?.toInt() == 1)
                                println("GameViewModel: Metadata update - completed: $value")
                                currentGame.copy(completed = value)
                            }
                            else -> currentGame
                        }
                    }
                }
            }

            // 2. Listen for Player-Specific Changes
            val playersRef = ref.child("players")
            launch {
                playersRef.childEvents().collect { event ->
                    val dto = try { event.snapshot.value<PlayerDTO>() } catch (e: Exception) { return@collect }
                    val remotePlayer = dto.toPlayer()

                    _game.update { currentGame ->
                        val currentList = currentGame.players.toMutableList()
                        val index = currentList.indexOfFirst { it.id == remotePlayer.id }

                        when (event.type) {
                            ChildEvent.Type.ADDED -> {
                                if (index == -1) {
                                    currentList.add(initializeHolesForPlayer(remotePlayer, currentGame.numberOfHoles))
                                }
                            }
                            ChildEvent.Type.CHANGED -> {
                                if (index != -1) {
                                    val localP = currentList[index]
                                    currentList[index] = mergePlayerImpl(localP, remotePlayer)
                                }
                            }
                            ChildEvent.Type.REMOVED -> {
                                if (index != -1) {
                                    currentList.removeAt(index)
                                }
                            }
                            else -> Unit
                        }
                        currentGame.copy(players = currentList)
                    }
                }
            }
        }
    }

    private fun mergePlayerImpl(local: Player, remote: Player): Player {
        val mergedHoles = mutableListOf<Hole>()
        for (remoteHole in remote.holes) {
            val localHole = local.holes.firstOrNull { it.number == remoteHole.number }
            if (localHole != null) {
                mergedHoles.add(localHole.copy(strokes = remoteHole.strokes))
            } else {
                mergedHoles.add(Hole(number = remoteHole.number, strokes = remoteHole.strokes))
            }
        }
        mergedHoles.sortBy { it.number }
        
        return local.copy(
            inGame = remote.inGame,
            holes = mergedHoles
        )
    }

    fun addLocalPlayer(name: String, email: String, ballColor: String? = null) {
        val newPlayer = Player(
            userId = generateGameCode(),
            name = name,
            email = email.takeIf { it.isNotBlank() },
            ballColorDT = ballColor,
            inGame = true
        )
        val playerWithHoles = initializeHolesForPlayer(newPlayer, _game.value.numberOfHoles)
        
        _game.value = _game.value.copy(
            players = _game.value.players + playerWithHoles
        )
        pushUpdate()
    }

    fun addUser(guestData: GuestData? = null) {
        if (guestData != null) {
            val newPlayer = Player(
                userId = guestData.id,
                name = guestData.name,
                email = guestData.email,
                ballColorDT = guestData.ballColorDT,
                inGame = true
            )
            val playerWithHoles = initializeHolesForPlayer(newPlayer, _game.value.numberOfHoles)
            _game.value = _game.value.copy(
                players = _game.value.players + playerWithHoles
            )
            pushUpdate()
        } else {
            val user = authModel.userModel.value ?: return
            if (isPlayerInGame(_game.value.players, user.googleId)) return
            
            val newPlayer = Player(
                userId = user.googleId,
                name = user.name,
                photoURL = user.photoURL,
                email = user.email,
                ballColorDT = user.ballColorDT,
                inGame = true
            )
            val playerWithHoles = initializeHolesForPlayer(newPlayer, _game.value.numberOfHoles)
            _game.value = _game.value.copy(
                players = _game.value.players + playerWithHoles
            )
            pushUpdate()
        }
    }

    fun removePlayer(userId: String) {
        _game.value = _game.value.copy(
            players = _game.value.players.filter { it.userId != userId }
        )
        pushUpdate()
    }

    fun joinGame(id: String, userId: String, completion: (Boolean, String?) -> Unit) {
        if (!_onlineGame.value) return
        resetGame()
        resetCourse()
        
        liveGameRepo.fetchGame(id) { game ->
            when (val status = validateJoinGame(game, userId)) {
                is JoinGameStatus.Success -> {
                    if (game != null) {
                        setGame(game)
                        addUser()
                        listenForUpdates()
                        completion(true, null)
                    }
                }
                is JoinGameStatus.Error -> {
                    completion(false, status.message)
                }
            }
        }
    }

    fun leaveGame(userId: String) {
        if (!_onlineGame.value) return
        
        _game.value = _game.value.copy(
            players = _game.value.players.filter { it.userId != userId }
        )
        pushUpdate()
        
        coroutineScope.launch {
            delay(500)
            stopListening()
            resetGame()
        }
    }

    fun createGame(online: Boolean = false, guestData: GuestData? = null) {
        setOnlineGame(online)
        if (_game.value.live) return
        
        resetGame()
        
        val hostId = guestData?.id ?: authModel.userModel.value?.googleId ?: ""
        val newId = generateGameCode()
        
        val setupGame = _game.value.copy(
            live = true,
            id = newId,
            hostUserId = hostId,
            courseID = _course.value?.id,
            locationName = _course.value?.name
        )
        _game.value = setupGame
        
        if (guestData != null) {
            addUser(guestData)
        } else {
            addUser()
        }
        
        pushUpdate()
        if (onlineGame.value && guestData == null) {
            listenForUpdates()
        }
    }

    fun startGame(onHostHidden: (Boolean) -> Unit) {
        if (_game.value.started) return
        
        val updatedPlayers = _game.value.players.map {
            initializeHolesForPlayer(it, _game.value.numberOfHoles)
        }
        
        _game.value = _game.value.copy(
            startTime = Timestamp.now(),
            started = true,
            players = updatedPlayers
        )

        if (onlineGame.value && (game.value.players.count({ it.userId.count() >= 7 }) < 2)) {
            setOnlineGame(false)
            stopListening()
            liveGameRepo.deleteGame(_game.value.id) { result ->
                if (result) {
                    println("Deleted Game id: \$gameIdToDelete From Firebase")
                }
            }
        }

        pushUpdate()
        onHostHidden(false)
    }

    fun dismissGame() {
        if (_game.value.dismissed || isDismissing) return
        
        isDismissing = true
        val gameIdToDelete = _game.value.id
        
        // Update state and PUSH to Firebase so guests see 'dismissed = true'
        _game.value = _game.value.copy(dismissed = true)
        if (_onlineGame.value && gameIdToDelete.isNotEmpty()) {
            pushUpdate()
        }

        hasLoaded = false
        
        coroutineScope.launch {
            // Wait for propagation before deleting node
            if (_onlineGame.value && gameIdToDelete.isNotEmpty()) {
                delay(1000)
            }
            stopListening()
            resetGame()
            
            if (gameIdToDelete.isNotEmpty() && _onlineGame.value) {
                liveGameRepo.deleteGame(gameIdToDelete) { result ->
                    if (result) {
                        println("Deleted Game id: $gameIdToDelete From Firebase")
                    }
                    isDismissing = false
                }
            } else {
                isDismissing = false
            }
        }
    }

    fun finishAndPersistGame(game: Game, isGuest: Boolean = false) {
        stopListening()
        
        val finished = game.copy(
            completed = true,
            endTime = Timestamp.now(),
            live = false
        )
        
        // Update local state and PUSH to Firebase so guests see 'completed = true'
        _game.value = finished
        if (_onlineGame.value && finished.id.isNotEmpty()) {
            pushUpdate()
        }

        coroutineScope.launch {
            var saveSuccess = false
            var analyticsSuccess = true

            if (!isGuest) {
                val (localOK, remoteOK) = suspendCancellableCoroutine<Pair<Boolean, Boolean>> { continuation ->
                    unifiedGameRepository.save(finished) { l, r ->
                        continuation.resume(Pair(l, r))
                    }
                }
                println("✅ Saved Game: local=$localOK, remote=$remoteOK")
                saveSuccess = localOK || remoteOK
            } else {
                val success = localGameRepository.save(finished)
                println(if (success) "✅ Saved Guest Game" else "❌ Failed to save guest game")
                saveSuccess = success
            }

            if (!saveSuccess) {
                println("⚠️ Skipping user save - game save failed")
                resetGameState()
                return@launch
            }

            val currentUserId = authModel.userModel.value?.googleId
            if (currentUserId != null && (currentUserId == finished.hostUserId || isGuest)) {
                println("running analytics")
                val success = processAnalytics(finished)
                println(if (success) "✅ Analytics processed" else "❌ Analytics failed")
                analyticsSuccess = success
            }

            val userModel = authModel.userModel.value
            val uid = authModel.currentUserIdentifier

            if (userModel != null && uid != null) {
                val updatedUserModel = userModel.copy(gameIDs = userModel.gameIDs + finished.id)
                val (localUserOK, remoteUserOK) = authModel.userRepository.saveUnified(updatedUserModel.googleId, updatedUserModel)
                authModel.setUserModel(updatedUserModel)
                val userSaveSuccess = localUserOK || remoteUserOK
                println(if (userSaveSuccess) "✅ Updated user model with new game ID" else "❌ Failed to update user model")
                if (!analyticsSuccess) {
                    println("⚠️ Analytics encountered issues, but game was saved")
                }
            } else {
                println("❌ Unable to save user model - missing userModel or currentUserIdentifier")
            }
            
            if (finished.id.isNotEmpty() && _onlineGame.value) {
                val result = suspendCancellableCoroutine<Boolean> { continuation ->
                    liveGameRepo.deleteGame(finished.id) {
                        continuation.resume(it)
                    }
                }
                if (result) {
                    println("Deleted Game id: ${finished.id} From Firebase Live DB")
                }
            }

            resetGameState()
        }
    }

    private fun resetGameState() {
        hasLoaded = false
        resetCourse()
        resetGame()
    }

    suspend fun processAnalytics(finishedGame: Game): Boolean {
        val courseID = finishedGame.courseID
        if (courseID == null) {
            println("No Course Id No Analytics")
            return false
        }

        val emails = finishedGame.players.mapNotNull { it.email }
        val result = analyticsRepo.updateDayAnalytics(
            emails = emails,
            courseID = courseID,
            game = finishedGame,
            startTime = finishedGame.startTime,
            endTime = finishedGame.endTime
        )
        return result.getOrDefault(false)
    }


    suspend fun findClosestLocationAndLoadCourse() {
        if (hasLoaded) return

        if (locationHandler.userLocation.value == null) {
            locationHandler.userLocation
                .filterNotNull() // Skip nulls
                .first()         // Suspend until the first non-null coordinate arrives
        }

        val closestPlace: MapItemDTO? = suspendCancellableCoroutine { continuation ->
            locationHandler.findClosestMiniGolf { place ->
                continuation.resume(place)
            }
        }

        val placeDTO = closestPlace ?: return
        val courseID = CourseIDGenerator.generateCourseID(placeDTO)

        val fetchedCourse: Course? = courseRepo.fetchCourse(id = courseID, mapItem = placeDTO)

        setCourse(fetchedCourse)
        setHasLoaded(true)
    }

    suspend fun setUp() {
        if (course.value == null && !hasLoaded) {
            findClosestLocationAndLoadCourse()
        }
    }

    suspend fun searchNearby(isLoading1: (Boolean) -> Unit, isLoading2: (Boolean) -> Unit) {
        isLoading1(true)
        setHasLoaded(false)
        try {
            findClosestLocationAndLoadCourse()
        } finally {
            isLoading2(true)
        }
    }


    fun exit(){
        resetCourse()
    }

    fun resetCourse() {
        setCourse(null)
        setGame(_game.value.copy(courseID = null))
    }

    suspend fun retry(firstRotate: (Boolean) -> Unit, secondRotate: (Boolean) -> Unit, isLoading1: (Boolean) -> Unit, isLoading2: (Boolean) -> Unit) {
        firstRotate(true)
        searchNearby(
            isLoading1 = isLoading1,
            isLoading2 = isLoading2
        )
        coroutineScope.launch {
            delay(1000L) // Wait for 1 second (1000 milliseconds)
            secondRotate(false)
        }
    }

    suspend fun fetchGuestGame(): Game?{
        return localGameRepository.fetchGuestGame()
    }


    fun generateGameCode(length: Int = 6): String {
        val chars = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun initializeHolesForPlayer(player: Player, totalHoles: Int): Player {
        if (player.holes.size == totalHoles) return player
        val existing = player.holes.map { it.number }.toSet()
        val newHoles = player.holes.toMutableList()
        for (n in 1..totalHoles) {
            if (n !in existing) {
                newHoles.add(Hole(number = n))
            }
        }
        newHoles.sortBy { it.number }
        return player.copy(holes = newHoles)
    }

    private fun isPlayerInGame(players: List<Player>, userId: String): Boolean {
        return players.any { it.userId == userId }
    }

    private fun validateJoinGame(game: Game?, userId: String): JoinGameStatus {
        if (game == null) {
            return JoinGameStatus.Error("Game not found. Please check the code and try again.")
        }
        if (game.dismissed) {
            return JoinGameStatus.Error("This game has been dismissed by the host.")
        }
        if (game.started) {
            return JoinGameStatus.Error("This game has already started.")
        }
        if (game.completed) {
            return JoinGameStatus.Error("This game has already been completed.")
        }
        if (isPlayerInGame(game.players, userId)) {
            return JoinGameStatus.Error("You are already in this game. Use a different account to join")
        }
        return JoinGameStatus.Success
    }
}

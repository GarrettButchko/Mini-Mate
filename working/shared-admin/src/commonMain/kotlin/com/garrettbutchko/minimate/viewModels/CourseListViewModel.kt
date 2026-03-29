package com.garrettbutchko.minimate.viewModels

import com.garrettbutchko.minimate.dataModels.courseModels.Course
import com.garrettbutchko.minimate.dataModels.courseModels.SocialPlatform
import com.garrettbutchko.minimate.repositories.CourseRepository
import com.garrettbutchko.minimate.repositories.userRepos.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Clock
import kotlin.time.Instant

open class CourseListViewModel(
    private val courseRepo: CourseRepository,
    private val userRepo: UserRepository,

    private val authModel: AuthViewModel,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    // MARK: - Published Properties
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _showAddCourseAlert = MutableStateFlow(false)
    val showAddCourseAlert: StateFlow<Boolean> = _showAddCourseAlert.asStateFlow()

    private val _loadingCourse = MutableStateFlow(false)
    val loadingCourse: StateFlow<Boolean> = _loadingCourse.asStateFlow()
    
    private val _userCourses = MutableStateFlow<List<Course>>(emptyList())
    val userCourses: StateFlow<List<Course>> = _userCourses.asStateFlow()

    private val _selectedCourse = MutableStateFlow<Course?>(null)
    val selectedCourse: StateFlow<Course?> = _selectedCourse.asStateFlow()

    private val _timeRemaining = MutableStateFlow(0.0)
    val timeRemaining: StateFlow<Double> = _timeRemaining.asStateFlow()

    private val _failedAttempts = MutableStateFlow(0)
    val failedAttempts: StateFlow<Int> = _failedAttempts.asStateFlow()

    private val _addTarget = MutableStateFlow<ColorAddTarget?>(null)
    val addTarget: StateFlow<ColorAddTarget?> = _addTarget.asStateFlow()

    private val _showColor = MutableStateFlow(false)
    val showColor: StateFlow<Boolean> = _showColor.asStateFlow()

    // MARK: - Dependencies
    private var saveJob: Job? = null
    private var timerJob: Job? = null
    private var listenJob: Job? = null

    val failedLimit: Int = 5
    private val ttl: Double = 30.0
    private var lastUpdated: Instant = Clock.System.now()

    val hasCourse: Boolean
        get() {
            val adminCourses = authModel?.userModel?.value?.adminCourses
            return !adminCourses.isNullOrEmpty()
        }

    val blockAddingCourse: Boolean
        get() = _failedAttempts.value >= failedLimit

    fun setPassword(value: String) { _password.value = value }
    fun setMessage(value: String?) { _message.value = value }
    fun setShowAddCourseAlert(value: Boolean) { _showAddCourseAlert.value = value }
    fun setAddTarget(value: ColorAddTarget?) { _addTarget.value = value }
    fun setShowColor(value: Boolean) { _showColor.value = value }
    fun setLoadingCourse(value: Boolean) { _loadingCourse.value = value }

    fun tick() {
        if (_timeRemaining.value <= 0) return

        val now = Clock.System.now()
        val elapsed = (now - lastUpdated).inWholeSeconds.toDouble()
        val remaining = max(0.0, ttl - elapsed)
        
        _timeRemaining.value = remaining

        if (remaining == 0.0) {
            _message.value = null
            _failedAttempts.value = 0
            timerJob?.cancel()
            timerJob = null
        }
    }

    fun startTimer() {
        if (_timeRemaining.value > 0) return
        lastUpdated = Clock.System.now()
        _timeRemaining.value = ttl
        
        timerJob?.cancel()
        timerJob = coroutineScope.launch {
            while (isActive && _timeRemaining.value > 0) {
                delay(1000)
                tick()
            }
        }
    }

    fun setCourse(course: Course?) {
        _selectedCourse.value = course
    }

    fun getCourses() {
        _loadingCourse.value = true
        val ids = authModel?.userModel?.value?.adminCourses
        
        if (!hasCourse || ids.isNullOrEmpty()) {
            _loadingCourse.value = false
            return
        }

        coroutineScope.launch {
            val courses = courseRepo.fetchCourses(ids)
            _userCourses.value = courses
            _loadingCourse.value = false
        }
    }

    fun getCourse(completion: () -> Unit) {
        _loadingCourse.value = true
        val firstCourseID = authModel?.userModel?.value?.adminCourses?.firstOrNull()
        
        if (!hasCourse || firstCourseID == null) {
            _loadingCourse.value = false
            completion()
            return
        }

        coroutineScope.launch {
            val course = courseRepo.fetchCourse(firstCourseID)
            if (course != null) {
                _userCourses.value = _userCourses.value + course
                _selectedCourse.value = course
            }
            _loadingCourse.value = false
            completion()
        }
    }

    fun tryPassword(completion: (Boolean) -> Unit) {
        coroutineScope.launch {
            val courseID = courseRepo.findCourseIDWithPassword(_password.value)
            
            if (courseID != null && authModel != null) {
                val currentUser = authModel!!.userModel.value
                val currentAdminCourses = currentUser?.adminCourses ?: emptyList()
                
                if (currentAdminCourses.contains(courseID)) {
                    _message.value = "Course Already Added"
                    completion(false)
                } else {
                    if (currentUser != null) {
                        val updatedUser = currentUser.copy(adminCourses = currentAdminCourses + courseID)
                        authModel!!.setUserModel(updatedUser)
                        
                        val currentUserId = authModel!!.currentUserIdentifier
                        if (currentUserId != null) {
                            userRepo.saveUnified(currentUserId, updatedUser)
                        }
                    }
                    
                    val email = authModel!!.firebaseUser.value?.email
                    if (email != null) {
                        val success = courseRepo.addAdminIDtoCourse(email, courseID)
                        if (success) {
                            println("✅ Course claimed and email added to adminIDs")
                        } else {
                            println("❌ Failed to add email to course adminIDs")
                        }
                    }
                    
                    getCourses()
                    _message.value = null
                    completion(true)
                }
            } else {
                _failedAttempts.value += 1
                
                if (_failedAttempts.value < failedLimit) {
                    _message.value = "Unsuccessful attempt. Please try again."
                } else {
                    _message.value = "Too many attempts"
                    startTimer()
                }
                completion(false)
            }
        }
    }

    fun start() {
        val course = _selectedCourse.value ?: return
        
        listenJob?.cancel()
        listenJob = coroutineScope.launch {
            courseRepo.listenToCourse(course.id).collect { newCourse ->
                if (_selectedCourse.value != newCourse) {
                    _selectedCourse.value = newCourse
                }
            }
        }
    }

    fun stop() {
        coroutineScope.launch {
            delay(50)
            listenJob?.cancel()
            listenJob = null
        }
    }

    // MARK: - Save Methods

    fun debouncedSave(delayMs: Long = 500) {
        val course = _selectedCourse.value ?: return
        saveJob?.cancel()
        saveJob = coroutineScope.launch {
            delay(delayMs)
            courseRepo.addOrUpdateCourse(course)
        }
    }

    fun immediateSave() {
        val course = _selectedCourse.value ?: return
        saveJob?.cancel()
        coroutineScope.launch {
            courseRepo.addOrUpdateCourse(course)
        }
    }

    // MARK: - State Update Methods for Android Compose (Equivalent to iOS Bindings)

    fun updateCourseField(debounce: Boolean = false, update: (Course) -> Course) {
        val currentCourse = _selectedCourse.value ?: return
        _selectedCourse.value = update(currentCourse)
        if (debounce) debouncedSave() else immediateSave()
    }

    fun updateOptionalCourseField(
        newValue: String?,
        deleteKey: String,
        debounce: Boolean = true,
        update: (Course, String?) -> Course
    ) {
        val currentCourse = _selectedCourse.value ?: return
        val valueToSave = newValue?.takeIf { it.isNotEmpty() }
        val updatedCourse = update(currentCourse, valueToSave)
        _selectedCourse.value = updatedCourse
        
        if (valueToSave == null) {
            coroutineScope.launch {
                courseRepo.deleteCourseItem(updatedCourse.id, deleteKey)
            }
        } else {
            if (debounce) debouncedSave() else immediateSave()
        }
    }

    fun updateSocialPlatform(index: Int, newPlatform: SocialPlatform, debounce: Boolean = false) {
        val currentCourse = _selectedCourse.value ?: return
        if (index !in currentCourse.socialLinks.indices) return
        
        val updatedLinks = currentCourse.socialLinks.toMutableList()
        updatedLinks[index] = updatedLinks[index].copy(platform = newPlatform)
        
        _selectedCourse.value = currentCourse.copy(socialLinks = updatedLinks)
        if (debounce) debouncedSave() else immediateSave()
    }

    fun updateLimitedCourseField(
        newValue: String?,
        limit: Int,
        deleteKey: String,
        debounce: Boolean = true,
        update: (Course, String?) -> Course
    ) {
        val currentCourse = _selectedCourse.value ?: return
        val limitedValue = newValue?.take(limit)
        val valueToSave = limitedValue?.takeIf { it.isNotEmpty() }
        
        val updatedCourse = update(currentCourse, valueToSave)
        _selectedCourse.value = updatedCourse
        
        if (valueToSave == null) {
            coroutineScope.launch {
                courseRepo.deleteCourseItem(updatedCourse.id, deleteKey)
            }
        } else {
            if (debounce) debouncedSave() else immediateSave()
        }
    }

    fun updateCustomPar(customPar: Boolean) {
        val currentCourse = _selectedCourse.value ?: return
        _selectedCourse.value = currentCourse.copy(
            customPar = customPar,
            numHoles = 18,
            pars = if (customPar) List(18) { 2 } else emptyList()
        )
        immediateSave()
    }

    fun updateNumHoles(newHoles: Int) {
        val currentCourse = _selectedCourse.value ?: return
        var pars = currentCourse.pars
        
        pars = if (newHoles > pars.size) {
            pars + List(newHoles - pars.size) { 2 }
        } else {
            pars.take(newHoles)
        }
        
        _selectedCourse.value = currentCourse.copy(
            numHoles = newHoles,
            pars = pars
        )
        immediateSave()
    }

    fun updatePar(index: Int, newPar: Int) {
        val currentCourse = _selectedCourse.value ?: return
        if (index !in currentCourse.pars.indices) return
        
        val updatedPars = currentCourse.pars.toMutableList()
        updatedPars[index] = newPar
        
        _selectedCourse.value = currentCourse.copy(pars = updatedPars)
        debouncedSave(300)
    }
}

package com.garrettbutchko.minimate.viewModels.analytics

import com.garrettbutchko.minimate.dataModels.courseModels.Course
import com.garrettbutchko.minimate.dataModels.courseModels.CourseEmail
import com.garrettbutchko.minimate.dataModels.courseModels.DailyDoc
import com.garrettbutchko.minimate.extensions.toLocalDateTime
import com.garrettbutchko.minimate.extensions.format
import com.garrettbutchko.minimate.generateUUID
import com.garrettbutchko.minimate.repositories.AnalyticsRepository
import com.garrettbutchko.minimate.repositories.analytics.AnalyticsRange
import dev.gitlive.firebase.firestore.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.collections.iterator
import kotlin.math.abs
import kotlin.math.max

// MARK: - Enums & Models
enum class AnalyticsSection(val rawValue: String) {
    GROWTH("Growth"),
    OPERATIONS("Operations"),
    EXPERIENCE("Experience"),
    RETENTION("Retention")
}

enum class ChartTopic(val title: String) {
    TOTAL("Total Visits"),
    FIRST("First Time Visits"),
    RETURNING("Returning Visits")
}

data class AnalyticsObject(
    val type: AnalyticsSection,
    val icon: String,
    val color: String
)

data class DataPointObject(
    val value: String,
    val delta: String? = null,
    val deltaColor: String? = null
)

data class PlayerActivity(
    val id: String = generateUUID(), // UUID as String
    val date: LocalDateTime,
    val count: Int
)

data class GameDurationActivity(
    val id: String, // UUID as String
    val date: LocalDateTime,
    val avgMinutes: Double
)

enum class InsightType {
    GOOD, AVERAGE, WARNING, CRITICAL
}

data class Insight(
    val description: String,
    val insightType: InsightType
)

data class SectionHealthRating(
    val section: AnalyticsSection,
    val score: Double,
    val grade: HealthGrade,
    val insights: List<Insight>,
    val metrics: Map<String, Double>
)

enum class HealthGrade(val rawValue: String) {
    EXCELLENT("A+"),
    GREAT("A"),
    GOOD("B+"),
    SATISFACTORY("B"),
    FAIR("C+"),
    NEEDS_IMPROVEMENT("C"),
    POOR("D"),
    CRITICAL("F");

    companion object {
        fun from(score: Double): HealthGrade {
            return when {
                score >= 95 -> EXCELLENT
                score >= 85 -> GREAT
                score >= 75 -> GOOD
                score >= 65 -> SATISFACTORY
                score >= 55 -> FAIR
                score >= 45 -> NEEDS_IMPROVEMENT
                score >= 30 -> POOR
                else -> CRITICAL
            }
        }
    }
}

data class CourseHealthReport(
    val overallScore: Double,
    val overallGrade: HealthGrade,
    val growthHealth: SectionHealthRating,
    val operationsHealth: SectionHealthRating,
    val experienceHealth: SectionHealthRating,
    val retentionHealth: SectionHealthRating,
    val topInsights: List<Pair<Insight, AnalyticsSection>>,
    val timestamp: LocalDateTime
)

data class HoleDifficultyData(
    val id: String = generateUUID(),
    val holeNumber: Int,
    val averageStrokes: Double
)

data class HoleHeatmapData(
    val id: String = generateUUID(),
    val holeNumber: Int,
    val relativeToPar: Double,
    val holePar: Int
)

// MARK: - Main ViewModel
open class AnalyticsViewModel(
    val analyticsRepo: AnalyticsRepository = AnalyticsRepository(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private val _range = MutableStateFlow<AnalyticsRange>(AnalyticsRange.Last30)
    val range: StateFlow<AnalyticsRange> = _range.asStateFlow()

    private val _selectedSection = MutableStateFlow(AnalyticsSection.GROWTH)
    val selectedSection: StateFlow<AnalyticsSection> = _selectedSection.asStateFlow()

    private val _pickedSection = MutableStateFlow("Day Range")
    val pickedSection: StateFlow<String> = _pickedSection.asStateFlow()

    val pickerSections: List<String> = listOf("Day Range", "Retention")

    private val _allDailyDocs = MutableStateFlow<List<DailyDoc>>(emptyList())
    val allDailyDocs: StateFlow<List<DailyDoc>> = _allDailyDocs.asStateFlow()

    val deltaDailyDocs: List<DailyDoc>
        get() {
            val days = _range.value.daysInDeltaRange
            return _allDailyDocs.value.filter { days.contains(it.dayID) }
        }

    val rangeDailyDocs: List<DailyDoc>
        get() {
            val days = _range.value.daysInMainRange
            return _allDailyDocs.value.filter { days.contains(it.dayID) }
        }

    private val _healthReport = MutableStateFlow<CourseHealthReport?>(null)
    val healthReport: StateFlow<CourseHealthReport?> = _healthReport.asStateFlow()

    private val _isLoadingHealth = MutableStateFlow(false)
    val isLoadingHealth: StateFlow<Boolean> = _isLoadingHealth.asStateFlow()

    private val _allEmails = MutableStateFlow<Map<String, CourseEmail>>(emptyMap())
    val allEmails: StateFlow<Map<String, CourseEmail>> = _allEmails.asStateFlow()

    private val _loadingDocs = MutableStateFlow(true)
    val loadingDocs: StateFlow<Boolean> = _loadingDocs.asStateFlow()

    private val _loadingEmails = MutableStateFlow(true)
    val loadingEmails: StateFlow<Boolean> = _loadingEmails.asStateFlow()

    private val _currentCourse = MutableStateFlow<Course?>(null)
    val currentCourse: StateFlow<Course?> = _currentCourse.asStateFlow()

    // Child View Models
    val growthVM = GrowthViewModel()
    val operationsVM = OperationsViewModel()
    val experienceVM = ExperienceViewModel()
    val retentionVM = RetentionViewModel()

    init {
        setupChildViewModels()
    }

    private fun setupChildViewModels() {
        coroutineScope.launch {
            _allDailyDocs.collect { docs ->
                val currentRange = _range.value
                val mainDays = currentRange.daysInMainRange
                val deltaDays = currentRange.daysInDeltaRange

                val rangeDocs = docs.filter { mainDays.contains(it.dayID) }
                val deltaDocs = docs.filter { deltaDays.contains(it.dayID) }

                growthVM.rangeDailyDocs = rangeDocs
                growthVM.deltaDailyDocs = deltaDocs
                operationsVM.rangeDailyDocs = rangeDocs
                operationsVM.deltaDailyDocs = deltaDocs
                experienceVM.rangeDailyDocs = rangeDocs
            }
        }

        coroutineScope.launch {
            _range.collect { newRange ->
                growthVM.range = newRange
                operationsVM.range = newRange

                val mainDays = newRange.daysInMainRange
                val deltaDays = newRange.daysInDeltaRange
                val docs = _allDailyDocs.value

                val rangeDocs = docs.filter { mainDays.contains(it.dayID) }
                val deltaDocs = docs.filter { deltaDays.contains(it.dayID) }

                growthVM.rangeDailyDocs = rangeDocs
                growthVM.deltaDailyDocs = deltaDocs
                operationsVM.rangeDailyDocs = rangeDocs
                operationsVM.deltaDailyDocs = deltaDocs
                experienceVM.rangeDailyDocs = rangeDocs
            }
        }

        coroutineScope.launch {
            _currentCourse.collect {
                experienceVM.currentCourse = it
            }
        }

        coroutineScope.launch {
            _allEmails.collect {
                retentionVM.allEmails = it
                retentionVM.recomputePlayerTiers()
            }
        }
    }

    fun loadHealthData(course: Course? = null) {
        _currentCourse.value = course
        if (course == null) {
            println("No course to load health data for")
            _healthReport.value = null
            return
        }

        _isLoadingHealth.value = true

        coroutineScope.launch(Dispatchers.Default) {
            val analyticsJob = async { onAppearDailyAnalyticsSuspend(course) }
            val retentionJob = async { onAppearRetentionSuspend(course) }

            analyticsJob.await()
            retentionJob.await()

            val report = calculateCourseHealthBackground()

            launch(Dispatchers.Main) {
                _healthReport.value = report
                _isLoadingHealth.value = false
            }
        }
    }

    fun positive(good: Boolean, delta: Double): String {
        return when {
            delta > 0 && good -> "GREEN"
            delta < 0 && !good -> "GREEN"
            delta < 0 && good -> "RED"
            delta > 0 && !good -> "RED"
            else -> "MAIN_OPP"
        }
    }

    val analyticsObjects: Map<String, AnalyticsObject> = mapOf(
        AnalyticsSection.GROWTH.rawValue to AnalyticsObject(
            type = AnalyticsSection.GROWTH,
            icon = "chart.line.uptrend.xyaxis",
            color = "GREEN"
        ),
        AnalyticsSection.OPERATIONS.rawValue to AnalyticsObject(
            type = AnalyticsSection.OPERATIONS,
            icon = "clock",
            color = "PURPLE"
        ),
        AnalyticsSection.EXPERIENCE.rawValue to AnalyticsObject(
            type = AnalyticsSection.EXPERIENCE,
            icon = "star",
            color = "PINK"
        )
    )

    fun onAppearDailyAnalytics(course: Course? = null) {
        if (course == null) return
        coroutineScope.launch {
            onAppearDailyAnalyticsSuspend(course)
        }
    }

    private suspend fun onAppearDailyAnalyticsSuspend(course: Course) {
        _loadingDocs.value = true
        _allDailyDocs.value = analyticsRepo.fetchDailyAnalytics(
            courseID = course.id,
            range = AnalyticsRange.Last30,
            existingDocs = _allDailyDocs.value
        )
        _loadingDocs.value = false
    }

    fun refreshAnalytics(course: Course?) {
        if (course == null) return
        _loadingDocs.value = true

        coroutineScope.launch {
            _allDailyDocs.value = analyticsRepo.fetchDailyAnalytics(
                courseID = course.id,
                range = AnalyticsRange.Last30,
                existingDocs = emptyList()
            )
            _loadingDocs.value = false
        }
    }

    fun onAppearRetention(course: Course?) {
        if (course == null) return
        coroutineScope.launch {
            onAppearRetentionSuspend(course)
        }
    }

    private suspend fun onAppearRetentionSuspend(course: Course) {
        _loadingEmails.value = true
        _allEmails.value = analyticsRepo.fetchEmails(courseID = course.id)
        retentionVM.recomputePlayerTiers()
        _loadingEmails.value = false
    }

    fun onChange(old: AnalyticsRange, new: AnalyticsRange, course: Course?) {
        if (course == null || old == new) return

        _loadingDocs.value = true

        coroutineScope.launch {
            _allDailyDocs.value = analyticsRepo.fetchDailyAnalytics(
                courseID = course.id,
                range = new,
                existingDocs = _allDailyDocs.value
            )
            _loadingDocs.value = false
        }
    }
    
    fun setRange(newRange: AnalyticsRange, course: Course?) {
        val old = _range.value
        _range.value = newRange
        onChange(old, newRange, course)
    }

    fun setPickedSection(section: String) {
        _pickedSection.value = section
    }

    fun setSelectedSection(section: AnalyticsSection) {
        _selectedSection.value = section
    }

    fun daysBetween(range: AnalyticsRange): Int {
        val dates = range.dates()
        return (dates.end.toEpochDays() - dates.start.toEpochDays()).toInt()
    }

    fun daysBetween(start: LocalDate, end: LocalDate): Int {
        return (end.toEpochDays() - start.toEpochDays()).toInt()
    }

    fun formatDateToMonthDay(date: LocalDate): String {
        val month = date.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        return "$month ${date.day}"
    }

    fun formatDateToDateString(date: LocalDate): String {
        return date.toString()
    }

    fun formatDateStringToMonthDay(dateString: String): LocalDate {
        return LocalDate.parse(dateString)
    }

    fun getDateRangeString(): String {
        val dates = _range.value.dates()
        val startS = formatDateToMonthDay(dates.start)
        val endS = formatDateToMonthDay(dates.end)
        return "$startS - $endS"
    }

    private fun calcDelta(prev: Int, current: Int): Double {
        if (prev == 0) return 0.0
        return ((current - prev).toDouble() / prev.toDouble()) * 100.0
    }

    // MARK: - Health Rating System
    private fun calculateCourseHealthBackground(): CourseHealthReport {
        val rangeDocs = rangeDailyDocs
        val deltaDocs = deltaDailyDocs
        val emails = _allEmails.value
        val course = _currentCourse.value
        val avgReturn = retentionVM.cachedAvgTimeToReturn
        val retention30 = retentionVM.cached30DayRetention
        val newPlayers = retentionVM.cachedNewPlayers
        val midTier = retentionVM.cachedMidTierPlayers
        val frequent = retentionVM.cachedFrequentPlayers
        val atRisk = retentionVM.cachedAtRiskPlayers

        val growthHealth = calculateGrowthHealthBackground(rangeDocs, deltaDocs)
        val operationsHealth = calculateOperationsHealthBackground(rangeDocs)
        val experienceHealth = calculateExperienceHealthBackground(rangeDocs, course)
        val retentionHealth = calculateRetentionHealthBackground(
            emails = emails,
            avgReturn = avgReturn,
            retention30 = retention30,
            newPlayers = newPlayers,
            midTier = midTier,
            frequent = frequent,
            atRisk = atRisk
        )

        val overallScore = (growthHealth.score * 0.30) +
                (operationsHealth.score * 0.20) +
                (experienceHealth.score * 0.20) +
                (retentionHealth.score * 0.30)

        val overallGrade = HealthGrade.from(overallScore)

        val topInsights = compileTopInsights(
            growthHealth, operationsHealth, experienceHealth, retentionHealth
        )

        return CourseHealthReport(
            overallScore = overallScore,
            overallGrade = overallGrade,
            growthHealth = growthHealth,
            operationsHealth = operationsHealth,
            experienceHealth = experienceHealth,
            retentionHealth = retentionHealth,
            topInsights = topInsights,
            timestamp = Timestamp.now().toLocalDateTime()
        )
    }

    fun calculateCourseHealth(): CourseHealthReport {
        return calculateCourseHealthBackground()
    }

    private fun calculateGrowthHealthBackground(rangeDocs: List<DailyDoc>, deltaDocs: List<DailyDoc>): SectionHealthRating {
        var score = 0.0
        val insights = mutableListOf<Insight>()
        val metrics = mutableMapOf<String, Double>()

        val activeUsers = rangeDocs.sumOf { it.totalCount }
        val activeUsersDelta = deltaDocs.sumOf { it.totalCount }
        val growthRate = if (activeUsersDelta > 0) calcDelta(activeUsersDelta, activeUsers) else 0.0
        metrics["growthRate"] = growthRate

        when {
            growthRate >= 20 -> {
                score += 40
                insights.add(Insight("Exceptional growth! Active users up +${growthRate.format(1)}%", InsightType.GOOD))
            }
            growthRate >= 10 -> {
                score += 35
                insights.add(Insight("Strong growth trend with +${growthRate.format(1)}% increase", InsightType.GOOD))
            }
            growthRate >= 5 -> {
                score += 30
                insights.add(Insight("Steady growth at ${growthRate.format(1)}%", InsightType.AVERAGE))
            }
            growthRate >= 0 -> {
                score += 20
                insights.add(Insight("Slow growth at ${growthRate.format(1)}%", InsightType.WARNING))
            }
            growthRate >= -10 -> {
                score += 10
                insights.add(Insight("Declining users by ${abs(growthRate).format(1)}%", InsightType.WARNING))
            }
            else -> {
                score += 0
                insights.add(Insight("Significant decline of ${abs(growthRate).format(1)}%", InsightType.CRITICAL))
            }
        }

        val firstTimeUsers = rangeDocs.sumOf { it.newPlayers }
        val newPlayerPercentage = if (activeUsers > 0) (firstTimeUsers.toDouble() / activeUsers) * 100 else 0.0
        metrics["newPlayerRate"] = newPlayerPercentage

        when {
            newPlayerPercentage >= 30 -> {
                score += 30
                insights.add(Insight("Excellent new player acquisition at ${newPlayerPercentage.format(0)}%", InsightType.GOOD))
            }
            newPlayerPercentage >= 20 -> {
                score += 25
                insights.add(Insight("Good new player rate at ${newPlayerPercentage.format(0)}%", InsightType.GOOD))
            }
            newPlayerPercentage >= 10 -> {
                score += 20
                insights.add(Insight("Moderate new player acquisition", InsightType.AVERAGE))
            }
            else -> {
                score += 10
                insights.add(Insight("Focus needed on attracting new players", InsightType.WARNING))
            }
        }

        val returningUsers = rangeDocs.sumOf { it.returningPlayers }
        val returningPercentage = if (activeUsers > 0) (returningUsers.toDouble() / activeUsers) * 100 else 0.0
        metrics["returningPlayerRate"] = returningPercentage

        when {
            returningPercentage >= 50 -> {
                score += 30
                insights.add(Insight("Outstanding player return rate at ${returningPercentage.format(0)}%", InsightType.GOOD))
            }
            returningPercentage >= 30 -> {
                score += 25
                insights.add(Insight("Healthy returning player base", InsightType.GOOD))
            }
            returningPercentage >= 15 -> {
                score += 15
                insights.add(Insight("Below-average return rate", InsightType.WARNING))
            }
            else -> {
                score += 5
                insights.add(Insight("Critical: Very low player retention", InsightType.CRITICAL))
            }
        }

        return SectionHealthRating(
            section = AnalyticsSection.GROWTH,
            score = score,
            grade = HealthGrade.from(score),
            insights = insights,
            metrics = metrics
        )
    }

    private fun calculateOperationsHealthBackground(rangeDocs: List<DailyDoc>): SectionHealthRating {
        var score = 0.0
        val insights = mutableListOf<Insight>()
        val metrics = mutableMapOf<String, Double>()

        val totalGames = rangeDocs.sumOf { it.gamesPlayed }
        val avgGamesPerDay = totalGames.toDouble() / max(1, rangeDocs.size)
        metrics["avgGamesPerDay"] = avgGamesPerDay

        when {
            avgGamesPerDay >= 50 -> {
                score += 40
                insights.add(Insight("High volume: ${avgGamesPerDay.format(0)} games/day", InsightType.GOOD))
            }
            avgGamesPerDay >= 25 -> {
                score += 35
                insights.add(Insight("Good activity with ${avgGamesPerDay.format(0)} games/day", InsightType.GOOD))
            }
            avgGamesPerDay >= 10 -> {
                score += 25
                insights.add(Insight("Moderate activity level", InsightType.AVERAGE))
            }
            avgGamesPerDay >= 5 -> {
                score += 15
                insights.add(Insight("Low game volume", InsightType.WARNING))
            }
            else -> {
                score += 5
                insights.add(Insight("Very low operational activity", InsightType.CRITICAL))
            }
        }

        val totalPlayers = rangeDocs.sumOf { it.totalCount }
        val avgPlayersPerGameValue = if (totalGames > 0) totalPlayers.toDouble() / totalGames.toDouble() else 0.0
        metrics["avgPlayersPerGame"] = avgPlayersPerGameValue

        when {
            avgPlayersPerGameValue >= 3 -> {
                score += 30
                insights.add(Insight("Excellent group sizes averaging ${avgPlayersPerGameValue.format(1)} players", InsightType.GOOD))
            }
            avgPlayersPerGameValue >= 2 -> {
                score += 25
                insights.add(Insight("Good social play with groups of ${avgPlayersPerGameValue.format(1)}", InsightType.AVERAGE))
            }
            avgPlayersPerGameValue >= 1.5 -> {
                score += 15
                insights.add(Insight("Opportunity to encourage group play", InsightType.WARNING))
            }
            else -> {
                score += 10
                insights.add(Insight("Promote social features to increase group sizes", InsightType.WARNING))
            }
        }

        val totalSeconds = rangeDocs.sumOf { it.totalRoundSeconds }
        val avgGameMinutes = if (totalGames > 0) totalSeconds.toDouble() / totalGames.toDouble() / 60.0 else 0.0
        metrics["avgGameMinutes"] = avgGameMinutes

        when {
            avgGameMinutes in 15.0..45.0 -> {
                score += 30
                insights.add(Insight("Optimal game duration at ${avgGameMinutes.format(0)} minutes", InsightType.GOOD))
            }
            avgGameMinutes >= 10 && avgGameMinutes < 15 -> {
                score += 25
                insights.add(Insight("Quick games averaging ${avgGameMinutes.format(0)} minutes", InsightType.AVERAGE))
            }
            avgGameMinutes > 45 && avgGameMinutes < 60 -> {
                score += 20
                insights.add(Insight("Longer games may indicate engagement or pacing issues", InsightType.WARNING))
            }
            else -> {
                if (avgGameMinutes > 0) {
                    score += 10
                    insights.add(Insight("Review game duration patterns", InsightType.WARNING))
                } else {
                    score += 15
                }
            }
        }

        return SectionHealthRating(
            section = AnalyticsSection.OPERATIONS,
            score = score,
            grade = HealthGrade.from(score),
            insights = insights,
            metrics = metrics
        )
    }

    private fun calculateExperienceHealthBackground(rangeDocs: List<DailyDoc>, course: Course?): SectionHealthRating {
        var score = 0.0
        val insights = mutableListOf<Insight>()
        val metrics = mutableMapOf<String, Double>()

        if (course == null) {
            return SectionHealthRating(
                section = AnalyticsSection.EXPERIENCE,
                score = 50.0,
                grade = HealthGrade.NEEDS_IMPROVEMENT,
                insights = listOf(Insight("Course data not available", InsightType.WARNING)),
                metrics = emptyMap()
            )
        }

        val combinedTotalStrokes = mutableMapOf<String, Int>()
        val combinedPlays = mutableMapOf<String, Int>()

        for (doc in rangeDocs) {
            for ((holeID, strokes) in doc.holeAnalytics.totalStrokesPerHole) {
                combinedTotalStrokes[holeID] = (combinedTotalStrokes[holeID] ?: 0) + strokes
            }
            for ((holeID, plays) in doc.holeAnalytics.playsPerHole) {
                combinedPlays[holeID] = (combinedPlays[holeID] ?: 0) + plays
            }
        }

        val holeCombined = mutableMapOf<String, Double>()
        for ((holeID, strokes) in combinedTotalStrokes) {
            holeCombined[holeID] = strokes.toDouble() / (combinedPlays[holeID] ?: 1).toDouble()
        }

        var totalOffset = 0.0
        var validHoleCount = 0

        for ((holeID, avgStrokes) in holeCombined) {
            val holeNum = holeID.toIntOrNull()
            if (holeNum != null && holeNum > 0 && holeNum <= course.pars.size) {
                val par = course.pars[holeNum - 1].toDouble()
                totalOffset += (avgStrokes - par)
                validHoleCount += 1
            }
        }

        val avgRelativeToPar = if (validHoleCount > 0) totalOffset / validHoleCount.toDouble() else 0.0
        metrics["avgRelativeToPar"] = avgRelativeToPar

        when {
            avgRelativeToPar in -0.5..0.5 -> {
                score += 35
                insights.add(Insight("Perfect difficulty balance! Players score near par", InsightType.GOOD))
            }
            avgRelativeToPar > 0.5 && avgRelativeToPar <= 1.5 -> {
                score += 30
                insights.add(Insight("Well-balanced challenge for players", InsightType.AVERAGE))
            }
            avgRelativeToPar > 1.5 && avgRelativeToPar <= 2.5 -> {
                score += 20
                insights.add(Insight("Course may be slightly too difficult", InsightType.WARNING))
            }
            avgRelativeToPar > 2.5 -> {
                score += 10
                insights.add(Insight("Course difficulty may frustrate players", InsightType.CRITICAL))
            }
            avgRelativeToPar < -0.5 -> {
                score += 25
                insights.add(Insight("Course may be too easy - consider adding challenges", InsightType.WARNING))
            }
            else -> {
                score += 15
            }
        }

        var totalHolesPlayed = 0
        var underParCount = 0

        for (doc in rangeDocs) {
            for ((holeID, totalStrokes) in doc.holeAnalytics.totalStrokesPerHole) {
                val holeNum = holeID.toIntOrNull()
                val plays = doc.holeAnalytics.playsPerHole[holeID]
                if (holeNum != null && holeNum > 0 && holeNum <= course.pars.size && plays != null && plays > 0) {
                    val par = course.pars[holeNum - 1].toDouble()
                    val avgStrokes = totalStrokes.toDouble() / plays.toDouble()

                    if (avgStrokes <= par) {
                        underParCount += plays
                    }
                    totalHolesPlayed += plays
                }
            }
        }

        val successRate = if (totalHolesPlayed > 0) (underParCount.toDouble() / totalHolesPlayed) * 100 else 0.0
        metrics["successRate"] = successRate

        when {
            successRate >= 30 -> {
                score += 35
                insights.add(Insight("Excellent player success rate at ${successRate.format(0)}%", InsightType.GOOD))
            }
            successRate >= 20 -> {
                score += 30
                insights.add(Insight("Good success rate keeps players engaged", InsightType.AVERAGE))
            }
            successRate >= 10 -> {
                score += 20
                insights.add(Insight("Moderate success rate - room for improvement", InsightType.WARNING))
            }
            else -> {
                score += 10
                insights.add(Insight("Low success rate may impact satisfaction", InsightType.WARNING))
            }
        }

        val holeVariety = holeCombined.values.sorted()
        if (holeVariety.size >= 2) {
            val rangeDiff = holeVariety.last() - holeVariety.first()
            metrics["difficultyVariety"] = rangeDiff

            when {
                rangeDiff >= 2 -> {
                    score += 30
                    insights.add(Insight("Excellent hole variety creates engaging experience", InsightType.GOOD))
                }
                rangeDiff >= 1 -> {
                    score += 25
                    insights.add(Insight("Good variety across holes", InsightType.AVERAGE))
                }
                else -> {
                    score += 15
                    insights.add(Insight("Consider adding more variety to hole difficulty", InsightType.WARNING))
                }
            }
        } else {
            score += 15
        }

        return SectionHealthRating(
            section = AnalyticsSection.EXPERIENCE,
            score = score,
            grade = HealthGrade.from(score),
            insights = insights,
            metrics = metrics
        )
    }

    private fun calculateRetentionHealthBackground(
        emails: Map<String, CourseEmail>,
        avgReturn: Int,
        retention30: Double,
        newPlayers: Map<String, CourseEmail>,
        midTier: Map<String, CourseEmail>,
        frequent: Map<String, CourseEmail>,
        atRisk: Map<String, CourseEmail>
    ): SectionHealthRating {
        var score = 0.0
        val insights = mutableListOf<Insight>()
        val metrics = mutableMapOf<String, Double>()

        val totalPlayers = emails.size.toDouble()
        if (totalPlayers <= 0) {
            return SectionHealthRating(
                section = AnalyticsSection.RETENTION,
                score = 0.0,
                grade = HealthGrade.CRITICAL,
                insights = listOf(Insight("No player data available", InsightType.WARNING)),
                metrics = emptyMap()
            )
        }

        val retention30Day = retention30 * 100.0
        metrics["retention30Day"] = retention30Day

        when {
            retention30Day >= 40 -> {
                score += 40
                insights.add(Insight("Outstanding 30-day retention at ${retention30Day.format(0)}%", InsightType.GOOD))
            }
            retention30Day >= 25 -> {
                score += 35
                insights.add(Insight("Strong retention rate of ${retention30Day.format(0)}%", InsightType.AVERAGE))
            }
            retention30Day >= 15 -> {
                score += 25
                insights.add(Insight("Moderate retention - opportunities exist", InsightType.WARNING))
            }
            retention30Day >= 10 -> {
                score += 15
                insights.add(Insight("Below-average retention needs attention", InsightType.WARNING))
            }
            else -> {
                score += 5
                insights.add(Insight("Critical: Very low player retention", InsightType.CRITICAL))
            }
        }

        val frequentCount = frequent.size.toDouble()
        val midTierCount = midTier.size.toDouble()
        val atRiskCount = atRisk.size.toDouble()

        val engagedRatio = ((frequentCount + midTierCount) / totalPlayers) * 100.0
        metrics["engagedPlayerRatio"] = engagedRatio

        when {
            engagedRatio >= 40 -> {
                score += 30
                insights.add(Insight("Exceptional engaged player base at ${engagedRatio.format(0)}%", InsightType.GOOD))
            }
            engagedRatio >= 25 -> {
                score += 25
                insights.add(Insight("Healthy mix of engaged players", InsightType.AVERAGE))
            }
            engagedRatio >= 15 -> {
                score += 15
                insights.add(Insight("Focus on moving players to higher tiers", InsightType.WARNING))
            }
            else -> {
                score += 8
                insights.add(Insight("Low engagement - activate dormant players", InsightType.WARNING))
            }
        }

        val atRiskRatio = (atRiskCount / totalPlayers) * 100.0
        metrics["atRiskRatio"] = atRiskRatio

        when {
            atRiskRatio < 15 -> {
                score += 30
                insights.add(Insight("Excellent retention - minimal at-risk players", InsightType.GOOD))
            }
            atRiskRatio < 30 -> {
                score += 25
                insights.add(Insight("Manageable at-risk player count", InsightType.AVERAGE))
            }
            atRiskRatio < 50 -> {
                score += 15
                insights.add(Insight("${atRiskRatio.format(0)}% players at risk - re-engagement needed", InsightType.WARNING))
            }
            else -> {
                score += 5
                insights.add(Insight("High churn risk: ${atRiskRatio.format(0)}% players at risk", InsightType.CRITICAL))
            }
        }

        metrics["avgReturnDays"] = avgReturn.toDouble()

        if (avgReturn < 7) {
            insights.add(Insight("Players return quickly (avg $avgReturn days)", InsightType.GOOD))
        } else if (avgReturn < 14) {
            insights.add(Insight("Good return frequency at $avgReturn days", InsightType.AVERAGE))
        } else if (avgReturn < 30) {
            insights.add(Insight("Consider incentives to shorten $avgReturn-day return cycle", InsightType.WARNING))
        } else {
            insights.add(Insight("Long return time ($avgReturn days) indicates engagement opportunity", InsightType.WARNING))
        }

        return SectionHealthRating(
            section = AnalyticsSection.RETENTION,
            score = score,
            grade = HealthGrade.from(score),
            insights = insights,
            metrics = metrics
        )
    }

    private fun compileTopInsights(
        growth: SectionHealthRating,
        operations: SectionHealthRating,
        experience: SectionHealthRating,
        retention: SectionHealthRating
    ): List<Pair<Insight, AnalyticsSection>> {
        val allInsights = mutableListOf<Triple<Insight, AnalyticsSection, Int>>()

        fun addInsights(insights: List<Insight>, section: AnalyticsSection) {
            for (insight in insights) {
                val priority = when (insight.insightType) {
                    InsightType.CRITICAL -> 1
                    InsightType.WARNING -> 2
                    else -> 3
                }
                allInsights.add(Triple(insight, section, priority))
            }
        }

        addInsights(growth.insights, AnalyticsSection.GROWTH)
        addInsights(operations.insights, AnalyticsSection.OPERATIONS)
        addInsights(experience.insights, AnalyticsSection.EXPERIENCE)
        addInsights(retention.insights, AnalyticsSection.RETENTION)

        allInsights.sortBy { it.third }

        return allInsights.take(7).map { Pair(it.first, it.second) }
    }
}

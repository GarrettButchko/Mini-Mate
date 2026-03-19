package com.garrettbutchko.minimate.repositories

import co.touchlab.kermit.Logger
import com.garrettbutchko.minimate.datamodels.Game
import com.garrettbutchko.minimate.datamodels.CourseEmail
import com.garrettbutchko.minimate.datamodels.DailyDoc
import com.garrettbutchko.minimate.datamodels.HoleAnalytics
import com.garrettbutchko.minimate.extensions.toInstant
import com.garrettbutchko.minimate.extensions.toLocalDate
import com.garrettbutchko.minimate.extensions.toLocalDateTime
import com.garrettbutchko.minimate.extensions.toTimestamp
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.Timestamp
import dev.gitlive.firebase.firestore.where
import dev.gitlive.firebase.firestore.FieldPath
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.*
import com.garrettbutchko.minimate.utilities.DateUtils

class AnalyticsRepository {

    private val db = Firebase.firestore
    private val collectionName = "courses"

    private val log = Logger.withTag("AnalyticsRepo")

    // MARK: - Update Day Analytics
    suspend fun updateDayAnalytics(
        emails: List<String>,
        courseID: String,
        game: Game,
        startTime: Timestamp,
        endTime: Timestamp
    ): Result<Boolean> {
        val updatedAt = Timestamp.now()
        val todayID = DateUtils.makeDayID(updatedAt)
        
        val currentHour = updatedAt.toLocalDateTime().hour

        val courseRef = db.collection(collectionName).document(courseID)
        val dayRef = courseRef.collection("dailyDocs").document(todayID)
        val emailRef = courseRef.collection("emails")

        val uniqueEmails = emails
            .map { it.lowercase().trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        if (uniqueEmails.isEmpty()) {
            log.i { "Empty Emails" }
            return Result.success(true)
        }

        return try {
            db.runTransaction {
                // --- 1. READ PHASE ---
                val emailSnapshots = uniqueEmails.associateWith { email ->
                    get(emailRef.document(emailKey(email)))
                }

                val daySnap = get(dayRef)
                val resultDay = if (daySnap.exists) daySnap.data<DailyDoc>() else null

                // --- 2. LOGIC PHASE ---
                var newCount = 0
                var returningCount = 0
                val emailUpdates = mutableListOf<Pair<String, CourseEmail>>()

                for (email in uniqueEmails) {
                    val snap = emailSnapshots[email]
                    if (snap != null && snap.exists) {
                        val data = snap.data<CourseEmail>()
                        var lastPlayed = data.lastPlayed ?: todayID
                        var secondSeen = data.secondSeen
                        val playCount = data.playCount

                        if (lastPlayed != todayID) {
                            returningCount += 1
                            lastPlayed = todayID
                        }
                        if (playCount == 1 && secondSeen == null) {
                            secondSeen = todayID
                        }

                        val updated = CourseEmail(
                            firstSeen = data.firstSeen ?: todayID,
                            secondSeen = secondSeen,
                            lastPlayed = lastPlayed,
                            playCount = playCount + 1
                        )
                        emailUpdates.add(emailKey(email) to updated)
                    } else {
                        newCount += 1
                        val updated = CourseEmail(
                            firstSeen = todayID,
                            secondSeen = null,
                            lastPlayed = todayID,
                            playCount = 1
                        )
                        emailUpdates.add(emailKey(email) to updated)
                    }
                }

                // Analytics calculations
                val roundLengthSeconds = (endTime.seconds - startTime.seconds).coerceAtLeast(0)
                val totalStrokes = resultDay?.holeAnalytics?.totalStrokesPerHole?.toMutableMap() ?: mutableMapOf()
                val playsPerHole = resultDay?.holeAnalytics?.playsPerHole?.toMutableMap() ?: mutableMapOf()
                val hourlyCounts = resultDay?.hourlyCounts?.toMutableMap() ?: mutableMapOf()

                for (player in game.players) {
                    for (h in player.holes) {
                        if (h.strokes == 0) continue
                        val key = h.number.toString()
                        totalStrokes[key] = (totalStrokes[key] ?: 0) + h.strokes
                        playsPerHole[key] = (playsPerHole[key] ?: 0) + 1
                    }
                }

                val hourKey = currentHour.toString()
                hourlyCounts[hourKey] = (hourlyCounts[hourKey] ?: 0) + 1

                val finalDailyDoc = DailyDoc(
                    dayID = todayID,
                    totalRoundSeconds = (resultDay?.totalRoundSeconds ?: 0L) + roundLengthSeconds,
                    gamesPlayed = (resultDay?.gamesPlayed ?: 0) + 1,
                    newPlayers = (resultDay?.newPlayers ?: 0) + newCount,
                    returningPlayers = (resultDay?.returningPlayers ?: 0) + returningCount,
                    holeAnalytics = HoleAnalytics(totalStrokesPerHole = totalStrokes, playsPerHole = playsPerHole),
                    hourlyCounts = hourlyCounts,
                    updatedAt = updatedAt
                )

                // --- 3. WRITE PHASE ---
                for ((key, obj) in emailUpdates) {
                    set(emailRef.document(key), obj, merge = true)
                }
                set(dayRef, finalDailyDoc, merge = true)
            }
            Result.success(true)
        } catch (e: Exception) {
            log.e(e) { "❌ updateDayAnalytics failed: ${e.message}" }
            Result.failure(e)
        }
    }

    fun emailKey(email: String): String {
        return email.lowercase().replace(".", ",")
    }

    fun emailFromKey(key: String): String {
        return key.replace(",", ".")
    }

    suspend fun fetchDailyAnalytics(
        courseID: String,
        range: AnalyticsRange,
        existingDocs: List<DailyDoc>
    ): List<DailyDoc> = coroutineScope {

        val dates = range.dates()
        val startDate = dates.start
        val endDate = dates.end
        val daysBetween = (endDate.toEpochDays() - startDate.toEpochDays())

        val extendedForData = startDate.minus(daysBetween, DateTimeUnit.DAY)

        val allDaysInFullRange = daysInRange(extendedForData, endDate)
        val existingDayIDs = existingDocs.map { it.dayID }.toSet()
        val missingDays = allDaysInFullRange.filter { !existingDayIDs.contains(it) }

        val fetchedDocs = mutableListOf<DailyDoc>()

        if (missingDays.isNotEmpty()) {
            val dailyDocsRef = db.collection(collectionName).document(courseID).collection("dailyDocs")
            try {
                val chunks = missingDays.chunked(30)
                val deferred = chunks.map { chunk ->
                    async {
                        dailyDocsRef.where { "dayID" inArray chunk }.get().documents.mapNotNull {
                            try { it.data<DailyDoc>() } catch (e: Exception) { null }
                        }
                    }
                }
                fetchedDocs.addAll(deferred.awaitAll().flatten())
            } catch (e: Exception) {
                log.e(e) { "❌ Firestore fetch failed: ${e.message}" }
            }
        }

        val combinedFoundDocs = existingDocs + fetchedDocs
        val docLookup = combinedFoundDocs.associateBy { it.dayID }

        allDaysInFullRange.map { dayID ->
            docLookup[dayID] ?: DailyDoc(dayID = dayID)
        }.sortedBy { it.dayID }
    }

    private fun daysInRange(startDate: LocalDate, endDate: LocalDate): List<String> {
        val days = mutableListOf<String>()
        var current = startDate
        while (current <= endDate) {
            days.add(DateUtils.makeDayID(current.toTimestamp()))
            current = current.plus(1, DateTimeUnit.DAY)
        }
        return days
    }

    private fun daysInRange(startDate: Timestamp, endDate: Timestamp): List<String> {
        return daysInRange(startDate.toLocalDate(), endDate.toLocalDate())
    }

    // MARK: - Debug Helpers
    suspend fun uploadDebugDailyDocs(
        courseID: String,
        days: Int = 90
    ): Result<Boolean> {
        val today = Timestamp.now().toLocalDate()
        val start = today.minus(days - 1, DateTimeUnit.DAY)
        val docs = makeDebugDailyDocs(start, today)

        if (docs.isEmpty()) return Result.success(true)

        return try {
            val batch = db.batch()
            val dailyDocsRef = db.collection(collectionName).document(courseID).collection("dailyDocs")
            for (doc in docs) {
                batch.set(dailyDocsRef.document(doc.dayID), doc, merge = true)
            }
            batch.commit()
            Result.success(true)
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to upload debug daily docs: ${e.message}" }
            Result.failure(e)
        }
    }

    private fun makeDebugDailyDocs(
        startDate: LocalDate,
        endDate: LocalDate,
        holes: Int = 18
    ): List<DailyDoc> {
        val docs = mutableListOf<DailyDoc>()
        var current = startDate
        while (current <= endDate) {
            val isoDay = current.dayOfWeek.isoDayNumber // 1 (Mon) - 7 (Sun)
            
            val dayWeight = when (isoDay) {
                6, 7 -> (1.8..2.5).random() // Busy Weekends
                5 -> (1.3..1.6).random() // Friday spike
                else -> (0.7..1.1).random() // Normal Weekdays
            }

            val baseGames = (15..25).random()
            val gamesPlayed = (baseGames.toDouble() * dayWeight).toInt()
            val playersPerGame = (2..4).random()
            val totalPlayers = gamesPlayed * playersPerGame
            val newPlayers = (totalPlayers.toDouble() * (0.4..0.7).random()).toInt()
            val returningPlayers = totalPlayers - newPlayers

            docs.add(makeDebugDailyDoc(
                dayID = current.toString(),
                holes = holes,
                players = playersPerGame,
                gamesPlayed = gamesPlayed,
                newPlayers = newPlayers,
                returningPlayers = returningPlayers,
                updatedAt = Timestamp(current.atStartOfDayIn(TimeZone.currentSystemDefault()).epochSeconds, 0)
            ))
            current = current.plus(1, DateTimeUnit.DAY)
        }
        return docs
    }

    private fun makeDebugDailyDoc(
        dayID: String,
        holes: Int,
        players: Int,
        gamesPlayed: Int,
        newPlayers: Int,
        returningPlayers: Int,
        updatedAt: Timestamp
    ): DailyDoc {
        val totalStrokes = mutableMapOf<String, Int>()
        val playsPerHole = mutableMapOf<String, Int>()

        for (hole in 1..holes) {
            val key = hole.toString()
            val plays = players * gamesPlayed
            playsPerHole[key] = plays

            val difficultyBias = when (hole) {
                6, 18 -> 2
                9 -> 1
                else -> 0
            }
            val avgStrokes = (3..5).random() + difficultyBias
            totalStrokes[key] = avgStrokes * plays
        }

        val hourlyCounts = mutableMapOf<String, Int>()
        val weights = mapOf(
            0 to 0.1, 1 to 0.0, 7 to 0.2, 8 to 0.8, 9 to 1.5, 10 to 2.0,
            11 to 3.0, 12 to 4.5, 13 to 4.0, 14 to 4.5, 15 to 5.0, 16 to 7.0,
            17 to 9.0, 18 to 10.0, 19 to 8.5, 20 to 6.0, 21 to 3.5, 22 to 1.5, 23 to 0.5
        )

        for (h in 0..23) hourlyCounts[h.toString()] = 0
        
        var gamesToDistribute = gamesPlayed
        while (gamesToDistribute > 0) {
            val hour = (0..23).random()
            val chance = weights[hour] ?: 0.0
            if ((0.0..10.0).random() < chance) {
                val hourKey = hour.toString()
                hourlyCounts[hourKey] = (hourlyCounts[hourKey] ?: 0) + 1
                gamesToDistribute--
            }
        }

        return DailyDoc(
            dayID = dayID,
            totalRoundSeconds = (70..100).random().toLong() * 60 * gamesPlayed,
            gamesPlayed = gamesPlayed,
            newPlayers = newPlayers,
            returningPlayers = returningPlayers,
            holeAnalytics = HoleAnalytics(totalStrokesPerHole = totalStrokes, playsPerHole = playsPerHole),
            hourlyCounts = hourlyCounts,
            updatedAt = updatedAt
        )
    }

    suspend fun fetchEmails(courseID: String): Map<String, CourseEmail> {
        val emailsRef = db.collection(collectionName).document(courseID).collection("emails")
        val emailsMap = mutableMapOf<String, CourseEmail>()
        var lastDoc: dev.gitlive.firebase.firestore.DocumentSnapshot? = null
        val pageSize = 30

        try {
            while (true) {
                var query = emailsRef.limit(pageSize.toLong())
                if (lastDoc != null) {
                    query = query.startAfter(lastDoc!!)
                }
                val snapshot = query.get()
                if (snapshot.documents.isEmpty()) break

                for (doc in snapshot.documents) {
                    try {
                        val email = doc.data<CourseEmail>()
                        val emailAddress = emailFromKey(doc.id)
                        emailsMap[emailAddress] = email
                    } catch (e: Exception) { }
                }

                lastDoc = snapshot.documents.lastOrNull()
                if (snapshot.documents.size < pageSize) break
            }
        } catch (e: Exception) {
            log.e(e) { "❌ Firestore fetch emails failed: ${e.message}" }
        }
        return emailsMap
    }

    suspend fun uploadDebugEmails(courseID: String, count: Int = 100): Result<Boolean> {
        val emails = makeDebugEmails(count)
        if (emails.isEmpty()) return Result.success(true)

        return try {
            val batch = db.batch()
            val emailRef = db.collection(collectionName).document(courseID).collection("emails")
            for ((email, data) in emails) {
                batch.set(emailRef.document(emailKey(email)), data, merge = true)
            }
            batch.commit()
            Result.success(true)
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to upload debug emails: ${e.message}" }
            Result.failure(e)
        }
    }

    private fun makeDebugEmails(count: Int): Map<String, CourseEmail> {
        val emails = mutableMapOf<String, CourseEmail>()
        val today = Timestamp.now().toLocalDate()

        val newCount = (count * 0.30).toInt()
        val midTierCount = (count * 0.25).toInt()
        val frequentCount = (count * 0.20).toInt()
        val atRiskCount = count - newCount - midTierCount - frequentCount

        var index = 1
        repeat(newCount) {
            val email = "new.player$index@example.com"
            val firstSeen = today.minus((1..30).random(), DateTimeUnit.DAY)
            emails[email] = CourseEmail(firstSeen.toString(), null, firstSeen.toString(), 1)
            index++
        }
        repeat(midTierCount) {
            val email = "midtier.player$index@example.com"
            val firstSeen = today.minus((30..90).random(), DateTimeUnit.DAY)
            val secondSeen = firstSeen.plus((5..25).random(), DateTimeUnit.DAY)
            val lastPlayed = today.minus((1..30).random(), DateTimeUnit.DAY)
            emails[email] = CourseEmail(firstSeen.toString(), secondSeen.toString(), lastPlayed.toString(), (2..5).random())
            index++
        }
        repeat(frequentCount) {
            val email = "frequent.player$index@example.com"
            val firstSeen = today.minus((60..180).random(), DateTimeUnit.DAY)
            val secondSeen = firstSeen.plus((5..25).random(), DateTimeUnit.DAY)
            val lastPlayed = today.minus((1..30).random(), DateTimeUnit.DAY)
            emails[email] = CourseEmail(firstSeen.toString(), secondSeen.toString(), lastPlayed.toString(), (6..20).random())
            index++
        }
        repeat(atRiskCount) {
            val email = "atrisk.player$index@example.com"
            val firstSeen = today.minus((90..365).random(), DateTimeUnit.DAY)
            val lastPlayed = today.minus((39..180).random(), DateTimeUnit.DAY)
            val secondSeen = firstSeen.plus((10..30).random(), DateTimeUnit.DAY).coerceAtMost(lastPlayed.minus(1, DateTimeUnit.DAY))
            emails[email] = CourseEmail(firstSeen.toString(), secondSeen.toString(), lastPlayed.toString(), (2..10).random())
            index++
        }
        return emails
    }

    private fun LocalDate.coerceAtMost(other: LocalDate): LocalDate = if (this > other) other else this
}

private val DayOfWeek.isoDayNumber: Int
    get() = when (this) {
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
        DayOfWeek.SUNDAY -> 7
    }

fun ClosedRange<Double>.random(): Double = (endInclusive - start) * kotlin.random.Random.nextDouble() + start
fun IntRange.random(): Int = kotlin.random.Random.nextInt(start, endInclusive + 1)

sealed class AnalyticsRange {
    object Last7 : AnalyticsRange()
    object Last30 : AnalyticsRange()
    object Last90 : AnalyticsRange()
    data class Custom(val start: LocalDate, val end: LocalDate) : AnalyticsRange()

    fun dates(now: LocalDate = Timestamp.now().toLocalDate()): RangeResult {
        return when (this) {
            Last7 -> {
                val start = now.minus(7, DateTimeUnit.DAY)
                RangeResult(start, now, now.minus(14, DateTimeUnit.DAY), start.minus(1, DateTimeUnit.DAY))
            }
            Last30 -> {
                val start = now.minus(30, DateTimeUnit.DAY)
                RangeResult(start, now, now.minus(60, DateTimeUnit.DAY), start.minus(1, DateTimeUnit.DAY))
            }
            Last90 -> {
                val start = now.minus(90, DateTimeUnit.DAY)
                RangeResult(start, now, now.minus(180, DateTimeUnit.DAY), start.minus(1, DateTimeUnit.DAY))
            }
            is Custom -> {
                val dayCount = (end.toEpochDays() - start.toEpochDays())
                val deltaEnd = start.minus(1, DateTimeUnit.DAY)
                val deltaStart = deltaEnd.minus(dayCount, DateTimeUnit.DAY)
                RangeResult(start, end, deltaStart, deltaEnd)
            }
        }
    }

    data class RangeResult(val start: LocalDate, val end: LocalDate, val dStart: LocalDate, val dEnd: LocalDate)
}

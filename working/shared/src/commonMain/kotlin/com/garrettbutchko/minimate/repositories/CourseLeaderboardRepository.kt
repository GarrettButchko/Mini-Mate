package com.garrettbutchko.minimate.repositories

import co.touchlab.kermit.Logger
import com.garrettbutchko.minimate.datamodels.LeaderboardEntry
import com.garrettbutchko.minimate.datamodels.Player
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.CollectionReference
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.min

class CourseLeaderboardRepository {

    private val db = Firebase.firestore

    // MARK: - Internal References

    private val log = Logger.withTag("CourseLeaderboardRepo")

    private fun allTimeEntriesRef(courseID: String): CollectionReference {
        return db.collection("courses").document(courseID)
            .collection("allTimeLeaderboard")
    }

    // MARK: - Fetch Data

    suspend fun fetchTopAllTime(courseID: String, limit: Int = 25): List<LeaderboardEntry> {
        return try {
            val snapshot = allTimeEntriesRef(courseID)
                .orderBy("totalStrokes", Direction.ASCENDING)
                .limit(limit.toLong())
                .get()
            snapshot.documents.map { it.data() }
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to fetch top all time leaderboard for course: $courseID" }
            emptyList()
        }
    }

    // MARK: - Live Listening

    fun listenTopAllTime(courseID: String, limit: Int = 25): Flow<List<LeaderboardEntry>> {
        return allTimeEntriesRef(courseID)
            .orderBy("totalStrokes", Direction.ASCENDING)
            .limit(limit.toLong())
            .snapshots
            .map { snapshot ->
                snapshot.documents.map { it.data<LeaderboardEntry>() }
            }
    }

    // MARK: - Submit Score

    suspend fun submitScore(courseID: String, player: Player): Result<Boolean> {
        val entry = player.toDTO().convertToLBREP() ?: return Result.success(false)
        val docRef = allTimeEntriesRef(courseID).document(entry.id)

        return try {
            db.runTransaction {
                val snapshot = get(docRef)
                
                val oldBest = if (snapshot.exists) {
                    // Explicitly ask for Int? to avoid "always non-null" warnings and handle missing fields
                    snapshot.get<Int?>("totalStrokes") ?: Int.MAX_VALUE
                } else {
                    Int.MAX_VALUE
                }

                val finalStrokes = if (entry.totalStrokes < oldBest) entry.totalStrokes else oldBest
                val finalEntry = entry.copy(totalStrokes = finalStrokes)

                set(docRef, finalEntry, merge = true)
            }
            Result.success(true)
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to submit score for course: $courseID, player: ${player.id}" }
            Result.failure(e)
        }
    }

    // MARK: - Delete Entry

    suspend fun deleteEntry(courseID: String, playerID: String): Result<Boolean> {
        return try {
            allTimeEntriesRef(courseID).document(playerID).delete()
            Result.success(true)
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to delete entry for course: $courseID, player: $playerID" }
            Result.failure(e)
        }
    }

    // MARK: - Bulk Delete

    suspend fun deleteAllEntries(courseID: String): Result<Boolean> {
        return try {
            val ref = allTimeEntriesRef(courseID)
            val snapshot = ref.get()
            
            if (snapshot.documents.isEmpty()) {
                return Result.success(true)
            }

            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit()

            Result.success(true)
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to delete all entries for course: $courseID" }
            Result.failure(e)
        }
    }
}
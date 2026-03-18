package com.garrettbutchko.minimate.repositories

import com.garrettbutchko.minimate.database.Game
import com.garrettbutchko.minimate.datamodels.GameDTO
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.FieldPath
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class RemoteGameRepository {

    private val db = Firebase.firestore
    private val collectionName = "games"

    // Save or update a game in Firestore
    suspend fun save(game: Game): Result<Boolean> {
        return try {
            db.collection(collectionName).document(game.id).set(game.toDTO(), merge = true)
            Result.success(true)
        } catch (e: Exception) {
            print("❌ Firestore save error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun save(games: List<Game>): Result<Boolean> = coroutineScope {
        if (games.isEmpty()) return@coroutineScope Result.success(true)

        try {
            games.map { game ->
                async {
                    db.collection(collectionName).document(game.id).set(game.toDTO(), merge = true)
                }
            }.awaitAll()
            Result.success(true)
        } catch (e: Exception) {
            print("❌ Firestore save error: ${e.message}")
            Result.failure(e)
        }
    }

    // Fetch a single game by ID
    suspend fun fetch(id: String): GameDTO? {
        return try {
            val snapshot = db.collection(collectionName).document(id).get()
            if (snapshot.exists) {
                snapshot.data<GameDTO>()
            } else {
                null
            }
        } catch (e: Exception) {
            print("❌ Firestore fetch error: ${e.message}")
            null
        }
    }

    suspend fun fetchAll(ids: List<String>): List<GameDTO> = coroutineScope {
        if (ids.isEmpty()) return@coroutineScope emptyList()

        // Firestore 'in' query supports up to 30 elements (used 10 in Swift snippet)
        val chunks = ids.chunked(10)

        try {
            val results = chunks.map { chunk ->
                async {
                    db.collection(collectionName)
                        .where { FieldPath.documentId inArray chunk }
                        .get()
                        .documents
                        .map { it.data<GameDTO>() }
                }
            }.awaitAll().flatten()

            // Sort by the input ids to maintain order as in the Swift implementation
            val gameMap = results.associateBy { it.id }
            ids.mapNotNull { gameMap[it] }
        } catch (e: Exception) {
            print("❌ Firestore fetchAll error: ${e.message}")
            emptyList()
        }
    }

    // Delete a game by ID
    suspend fun delete(id: String): Result<Boolean> {
        return try {
            db.collection(collectionName).document(id).delete()
            Result.success(true)
        } catch (e: Exception) {
            print("❌ Firestore delete error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteAll(ids: List<String>): Result<Boolean> {
        if (ids.isEmpty()) return Result.success(true)

        return try {
            val batch = db.batch()
            ids.forEach { id ->
                val ref = db.collection(collectionName).document(id)
                batch.delete(ref)
            }
            batch.commit()
            Result.success(true)
        } catch (e: Exception) {
            print("❌ Firestore batch delete error: ${e.message}")
            Result.failure(e)
        }
    }
}

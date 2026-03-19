package com.garrettbutchko.minimate.repositories

import co.touchlab.kermit.Logger
import com.garrettbutchko.minimate.datamodels.Course
import com.garrettbutchko.minimate.datamodels.MapItemDTO
import com.garrettbutchko.minimate.dataModels.courseModels.SmallCourse
import com.garrettbutchko.minimate.utilities.PasswordGenerator
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FieldPath
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import dev.gitlive.firebase.firestore.FieldValue.Companion.delete
import dev.gitlive.firebase.storage.Data

class CourseRepository {

    private val db = Firebase.firestore
    private val storage = Firebase.storage
    private val collectionName = "courses"

    private val log = Logger.withTag("CourseRepo")

    // MARK: General Course
    suspend fun addOrUpdateCourse(course: Course): Result<Boolean> {
        val ref = db.collection(collectionName).document(course.id)
        
        // Update computed properties before saving
        course.updateComputedProperties()
        
        return try {
            ref.set(course, merge = true)
            Result.success(true)
        } catch (e: Exception) {
            log.e(e) { "❌ Firestore encoding error:" + e.message }
            Result.failure(e)
        }
    }

    suspend fun deleteCourseItem(courseID: String, dataName: String): Result<Boolean> {
        val ref = db.collection("courses").document(courseID)

        return try {
            // In GitLive SDK, you use 'delete' inside updateFields
            ref.updateFields {
                dataName to delete
            }

            Result.success(true)
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to delete course item: " + e.message }
            Result.failure(e)
        }
    }
    suspend fun setCourseItem(courseID: String, dataName: String, value: Any): Result<Boolean> {
        val ref = db.collection("courses").document(courseID)

        return try {
            // In GitLive SDK, you use 'delete' inside updateFields
            ref.updateFields {
                dataName to value
            }
            Result.success(true)
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to set course item: " + e.message }
            Result.failure(e)
        }
    }

    fun listenToCourse(id: String): Flow<Course?> {
        return db.collection(collectionName).document(id).snapshots.map { snapshot ->
            if (snapshot.exists) snapshot.data<Course>() else null
        }
    }

    suspend fun fetchCourse(id: String, mapItem: MapItemDTO? = null): Course? {
        val ref = db.collection(collectionName).document(id)
        return try {
            val snapshot = ref.get()
            if (snapshot.exists) {
                snapshot.data<Course>()
            } else {
                mapItem?.let { createCourseWithMapItem(id, it) }
            }
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to fetch course: " + e.message }
            null
        }
    }

    suspend fun fetchCourses(ids: List<String>): List<Course> = coroutineScope {
        ids.map { id ->
            async { fetchCourse(id) }
        }.awaitAll().filterNotNull()
    }

    suspend fun fetchCourseByName(name: String): Course? {
        return try {
            db.collection(collectionName)
                .where { "name" equalTo name }
                .limit(1)
                .get()
                .documents.firstOrNull()?.data()
        } catch (e: Exception) {
            log.e(e) { "❌ Firestore query error:" + e.message }
            null
        }
    }

    suspend fun courseNameExistsAndSupported(name: String): Boolean {
        return try {
            val snapshot = db.collection(collectionName)
                .where { 
                    all(
                        "name" equalTo name,
                        "isSupported" equalTo true
                    )
                }
                .limit(1)
                .get()
            !snapshot.documents.isEmpty()
        } catch (e: Exception) {
            log.e(e) { "❌ Firestore query error:" + e.message }
            false
        }
    }

    suspend fun createCourseWithMapItem(courseID: String, location: MapItemDTO): Course? {
        val ref = db.collection(collectionName).document(courseID)
        val newCourse = Course(
            id = courseID,
            name = location.name ?: "N/A",
            password = PasswordGenerator.generateStrong(),
            latitude = location.coordinate.latitude,
            longitude = location.coordinate.longitude
        )
        return try {
            ref.set(newCourse)
            log.i { "Created new course:$courseID" }
            newCourse
        } catch (e: Exception) {
            log.e(e) { "❌ Firestore write error:" + e.message }
            null
        }
    }

    suspend fun findCourseIDWithPassword(password: String): String? {
        return try {
            db.collection(collectionName)
                .where { "password" equalTo password }
                .limit(1)
                .get()
                .documents.firstOrNull()?.id
        } catch (e: Exception) {
            log.e(e) { "❌ Firestore query error:" + e.message }
            null
        }
    }

    suspend fun fetchCourseIDs(prefix: String): List<SmallCourse> {
        val end = prefix + "\uf8ff"
        return try {
            db.collection(collectionName)
                .where { 
                    all(
                        FieldPath.documentId greaterThanOrEqualTo prefix,
                        FieldPath.documentId lessThanOrEqualTo end
                    )
                }
                .limit(50)
                .get()
                .documents.map { doc ->
                    val name = doc.get<String?>("name") ?: "Unnamed"
                    SmallCourse(id = doc.id, name = name)
                }
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to fetch course IDs: " + e.message }
            emptyList()
        }
    }

    fun emailKey(email: String): String = email.lowercase().replace(".", ",")

    // MARK: Email
    suspend fun removeEmail(email: String, courseID: String): Boolean {
        val key = emailKey(email)
        try {
            db.collection(collectionName).document(courseID).updateFields {
                "emails.$key" to delete
            }
            return true
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to remove email:" + e.message }
            return false
        }
    }


    // MARK: Admin Id
    suspend fun addAdminIDtoCourse(adminID: String, courseID: String): Boolean {
        try {
            db.collection(collectionName).document(courseID).updateFields {
                "adminIDs" to FieldValue.arrayUnion(adminID)
                "isClaimed" to true
            }
            return true
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to add admin ID:" + e.message }
            return false
        }
    }

    suspend fun removeAdminIDfromCourse(email: String, courseID: String): Boolean {
        val ref = db.collection(collectionName).document(courseID)
        try {
            ref.updateFields {
                "adminIDs" to FieldValue.arrayRemove(email)
            }
            val snapshot = ref.get()
            val adminIDs = snapshot.get<List<String>?>("adminIDs") ?: emptyList()
            ref.updateFields {
                "isClaimed" to adminIDs.isNotEmpty()
            }
            return true
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to remove admin ID:" + e.message }
            return false
        }
    }

    suspend fun uploadCourseImage(id: String, imageData: Data, key: String): String? {
        val ref = storage.reference.child(id).child("$key.png")
        try {
            ref.putData(imageData)
            return ref.getDownloadUrl()
        } catch (e: Exception) {
            log.e(e) { "❌ Failed to upload course image:" + e.message }
            return null
        }
    }
}

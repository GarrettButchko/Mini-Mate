package com.garrettbutchko.minimate.datamodels

import com.garrettbutchko.minimate.generateUUID
import com.garrettbutchko.minimate.utilities.DateUtils.getWeekday
import com.garrettbutchko.minimate.utilities.DateUtils.getIsoWeekID
import kotlinx.serialization.Serializable

import dev.gitlive.firebase.firestore.Timestamp

@Serializable
data class Course(
    val id: String = "",
    var name: String = "",
    var password: String = "",

    var logo: String? = null,
    var scoreCardColorDT: String? = null,
    var courseColorsDT: List<String>? = null,

    var customPar: Boolean = false,
    var numHoles: Int = 18,
    var pars: List<Int> = emptyList(),

    var socialLinks: List<SocialLink> = emptyList(),

    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var isSeasonal: Boolean? = null,
    var indoor: Boolean? = null,

    var leaderBoardActive: Boolean = false,
    var tier: Int = 1,
    var adminIDs: List<String> = emptyList(),

    var isClaimed: Boolean = false,
    var isSupported: Boolean = false,
    // Custom Ad
    var customAdActive: Boolean = false,
    var adTitle: String? = null,
    var adDescription: String? = null,
    var adLink: String? = null,
    var adImage: String? = null,
    var adClicks: Int? = null
) {
    fun updateComputedProperties() {
        this.isClaimed = adminIDs.isNotEmpty()
        this.isSupported = customPar && (scoreCardColorDT != null || logo != null)
    }

    companion object {
        // Example of how you will use Firestore in the shared module:
        // suspend fun fetchCourse(id: String): Course {
        //     val db = Firebase.firestore
        //     return db.collection("courses").document(id).get().data()
        // }
    }
}


@Serializable
data class DailyDoc(
    var dayID: String, 
    var totalRoundSeconds: Long = 0,
    var gamesPlayed: Int = 0,
    var newPlayers: Int = 0,
    var returningPlayers: Int = 0,

    var holeAnalytics: HoleAnalytics = HoleAnalytics(),
    var hourlyCounts: Map<String, Int> = emptyMap(),
    var updatedAt: Timestamp? = null
) {
    var weekID: String = getIsoWeekID(dayID)
    var weekDay: Int = getWeekday(dayID)

    val totalCount: Int
        get() = newPlayers + returningPlayers

    val avgRoundTimeSeconds: Double
        get() = if (gamesPlayed > 0) totalRoundSeconds.toDouble() / gamesPlayed else 0.0
}

@Serializable
data class HoleAnalytics(
    var totalStrokesPerHole: Map<String, Int> = emptyMap(),
    var playsPerHole: Map<String, Int> = emptyMap()
)

@Serializable
data class CourseEmail (
    var firstSeen: String?,
    var secondSeen: String?,
    var lastPlayed: String?,
    var playCount: Int = 0
)

@Serializable
enum class SocialPlatform {
    INSTAGRAM, FACEBOOK, TIKTOK, YOUTUBE, WEBSITE
}

@Serializable
data class SocialLink(
    val id: String = generateUUID(),
    val platform: SocialPlatform,
    val url: String
)

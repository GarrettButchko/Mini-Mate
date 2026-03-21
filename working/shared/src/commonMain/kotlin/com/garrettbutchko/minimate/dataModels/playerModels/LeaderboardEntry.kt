package com.garrettbutchko.minimate.dataModels.playerModels

data class LeaderboardEntry(
    var id: String,
    var userId: String,
    var name: String,
    var photoURL: String?,
    var ballColorDT: String?,
    var totalStrokes: Int,
    var email: String
)

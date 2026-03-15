package com.garrettbutchko.minimate

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun generateUUID(): String

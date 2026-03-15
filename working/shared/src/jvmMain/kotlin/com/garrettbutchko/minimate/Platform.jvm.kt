package com.garrettbutchko.minimate

import java.util.UUID

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun generateUUID(): String = UUID.randomUUID().toString()

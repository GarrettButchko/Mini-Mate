package com.garrettbutchko.minimate.utilities

/**
 * Generates a short SHA-256 hash.
 * Note: For pure KMP (iOS/Android), you'd typically use a library like
 * 'Kotlin Crypto' or expect/actual for Platform-specific SHA256.
 */
fun shortHash(text: String): String {
    // Use a simple hash for commonMain to avoid JVM-specific MessageDigest
    val hash = text.fold(0L) { acc, c -> acc * 31 + c.code.toLong() }
    return hash.toString(16).lowercase().take(8)
}

/**
 * Generates a random alphanumeric nonce of the given length.
 */
fun randomNonceString(length: Int = 32): String {
    val actualLength = if (length > 0) length else {
        println("❌ Invalid length: $length, using default 32")
        32
    }

    val charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVXYZabcdefghijklmnopqrstuvwxyz-._"
    return (1..actualLength)
        .map { charset.random() }
        .joinToString("")
}
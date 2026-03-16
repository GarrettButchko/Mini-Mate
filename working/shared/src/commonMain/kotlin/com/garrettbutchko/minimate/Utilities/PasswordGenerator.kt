package com.garrettbutchko.minimate.utilities

import kotlin.random.Random
import kotlin.math.max

object PasswordGenerator {

    sealed class Style {
        data class Strong(val length: Int = 20, val useSymbols: Boolean = true) : Style()
        data class Memorable(val length: Int = 16, val includeDigits: Boolean = true) : Style()
    }

    // Character Sets
    private val uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toList()
    private val lowercase = "abcdefghijklmnopqrstuvwxyz".toList()
    private val digits = "0123456789".toList()
    private val symbols = "!@#$%&".toList()

    // Public API
    fun generate(style: Style): String {
        return when (style) {
            is Style.Strong -> generateStrongInternal(style.length, style.useSymbols)
            is Style.Memorable -> generateMemorable(style.length, style.includeDigits)
        }
    }

    // Helper for Swift
    fun generateStrong(length: Int = 20, useSymbols: Boolean = true): String {
        return generateStrongInternal(length, useSymbols)
    }

    // Strong Password
    private fun generateStrongInternal(length: Int, useSymbols: Boolean): String {
        val finalLength = max(length, 4)

        val sets = mutableListOf(uppercase, lowercase, digits).apply {
            if (useSymbols) add(symbols)
        }

        val pool = sets.flatten()

        // Ensure at least one character from each chosen set
        val result = sets.map { it[Random.nextInt(it.size)] }.toMutableList()

        // Fill the rest
        while (result.size < finalLength) {
            result.add(pool[Random.nextInt(pool.size)])
        }

        // Shuffle with Fisher-Yates
        for (i in result.indices.reversed()) {
            val j = Random.nextInt(i + 1)
            val temp = result[i]
            result[i] = result[j]
            result[j] = temp
        }

        return result.joinToString("")
    }

    // Memorable Password
    private fun generateMemorable(length: Int, includeDigits: Boolean): String {
        val finalLength = max(length, 4)
        val consonants = "bcdfghjklmnpqrstvwxyz".toList()
        val vowels = "aeiou".toList()

        val output = StringBuilder()
        var useConsonant = Random.nextInt(2) == 0

        while (output.length < finalLength) {
            val syllableLength = Random.nextInt(2) + 1

            for (i in 0 until syllableLength) {
                if (output.length >= finalLength) break
                val source = if (useConsonant) consonants else vowels
                output.append(source[source.size])
                useConsonant = !useConsonant
            }

            if (includeDigits && output.length < finalLength && Random.nextInt(8) == 0) {
                output.append(digits[Random.nextInt(digits.size)])
            }
        }

        return output.toString().take(finalLength)
    }
}

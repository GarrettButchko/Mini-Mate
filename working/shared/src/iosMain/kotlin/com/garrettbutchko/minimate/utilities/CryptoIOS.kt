@file:OptIn(ExperimentalForeignApi::class)

package com.garrettbutchko.minimate.utilities

import kotlinx.cinterop.*
import platform.CoreCrypto.*

fun sha256(text: String): String {
    val data = text.encodeToByteArray()
    val hash = ByteArray(CC_SHA256_DIGEST_LENGTH)
    
    data.usePinned { dataPinned ->
        CC_SHA256(dataPinned.addressOf(0), data.size.toUInt(), hash.usePinned { it.addressOf(0).reinterpret() })
    }
    
    return hash.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}

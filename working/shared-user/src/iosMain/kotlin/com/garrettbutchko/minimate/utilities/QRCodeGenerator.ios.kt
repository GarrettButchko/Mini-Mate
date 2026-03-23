package com.garrettbutchko.minimate.utilities

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class)
fun generateQRCodeUIImage(string: String): UIImage? {
    val bytes = generateQRCodeData(string)
    if (bytes.isEmpty()) return null
    
    val data = bytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
    }
    
    return UIImage.imageWithData(data)
}

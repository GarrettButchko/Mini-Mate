package com.garrettbutchko.minimate.viewModels

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toUIImage(): UIImage? {
    if (this.isEmpty()) return null
    
    val data = this.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
    }
    
    return UIImage.imageWithData(data)
}

fun HostViewModel.qrCodeUIImage(): UIImage? {
    return qrCodeImage.value?.toUIImage()
}

package com.garrettbutchko.minimate.utilities

import qrcode.QRCode

fun generateQRCodeData(string: String): ByteArray {
    return QRCode.ofSquares()
        .build(string)
        .renderToBytes()
}

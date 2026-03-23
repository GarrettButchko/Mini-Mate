package com.garrettbutchko.minimate.utilities

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream

actual fun generateQRCodeData(string: String): ByteArray {
    val bitmap = generateQRCodeBitmap(string) ?: return ByteArray(0)
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

fun generateQRCodeBitmap(string: String): Bitmap? {
    // Note: Android does not have a native QR code generator built into the OS.
    // To implement this fully on Android, you will need to add a library such as ZXing to your common/android dependencies.
    // Example: implementation("com.google.zxing:core:3.5.3")
    //
    // Then you can uncomment the following code:
    /*
    try {
        val hints = mapOf(
            com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M
        )
        val bitMatrix = com.google.zxing.MultiFormatWriter().encode(
            string,
            com.google.zxing.BarcodeFormat.QR_CODE,
            512,
            512,
            hints
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
    */
    return null
}

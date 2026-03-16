package com.garrettbutchko.minimate.ModelKotlinExtensions

import androidx.compose.ui.graphics.Color

fun String?.toColor(): Color {
    return when (this?.lowercase()) {
        "red" -> Color(0xFFFF383C)
        "orange" -> Color(0xFFFF8D28)
        "yellow" -> Color(0xFFFFCC00)
        "green" -> Color(0xFF34C759)
        "mint" -> Color(0xFF00C8B3)
        "teal" -> Color(0xFF00C3D0)
        "cyan" -> Color(0xFF00C0E8)
        "blue" -> Color(0xFF0088FF)
        "indigo" -> Color(0xFF6155F5)
        "purple" -> Color(0xFFCB30E0)
        "pink" -> Color(0xFFFF2D55)
        "brown" -> Color(0xFFAC7F5E)
        else -> Color.Gray // Fallback for unspecified or unknown colors
    }
}

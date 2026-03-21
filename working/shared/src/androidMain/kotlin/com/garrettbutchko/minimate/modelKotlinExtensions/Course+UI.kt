package com.garrettbutchko.minimate.modelKotlinExtensions

import androidx.compose.ui.graphics.Color
import com.garrettbutchko.minimate.dataModels.courseModels.Course

val Course.scoreCardColor: Color?
    get() = scoreCardColorDT?.toColor()?.copy(alpha = 0.4f)

val Course.courseColors: List<Color>?
    get() = courseColorsDT?.mapNotNull { it.toColor() }?.takeIf { it.isNotEmpty() }

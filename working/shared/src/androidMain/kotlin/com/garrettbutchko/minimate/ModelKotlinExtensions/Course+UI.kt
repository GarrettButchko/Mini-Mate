package com.garrettbutchko.minimate.ModelKotlinExtensions

import androidx.compose.ui.graphics.Color
import com.garrettbutchko.minimate.datamodels.Course

val Course.scoreCardColor: Color?
    get() = scoreCardColorDT?.toColor()?.copy(alpha = 0.4f)

val Course.courseColors: List<Color>?
    get() = courseColorsDT?.mapNotNull { it.toColor() }?.takeIf { it.isNotEmpty() }

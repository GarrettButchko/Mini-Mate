package com.garrettbutchko.minimate.ModelKotlinExtensions

import androidx.compose.ui.graphics.Color
import com.garrettbutchko.minimate.datamodels.Player
import com.garrettbutchko.minimate.datamodels.PlayerDTO

val Player.ballColor: Color
    get() = ballColorDT.toColor()


val PlayerDTO.ballColor: Color
    get() = ballColorDT.toColor()


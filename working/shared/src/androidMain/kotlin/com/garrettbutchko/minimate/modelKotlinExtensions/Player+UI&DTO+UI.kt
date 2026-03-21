package com.garrettbutchko.minimate.modelKotlinExtensions

import androidx.compose.ui.graphics.Color
import com.garrettbutchko.minimate.dataModels.playerModels.Player
import com.garrettbutchko.minimate.dataModels.playerModels.PlayerDTO

val Player.ballColor: Color
    get() = ballColorDT.toColor()


val PlayerDTO.ballColor: Color
    get() = ballColorDT.toColor()

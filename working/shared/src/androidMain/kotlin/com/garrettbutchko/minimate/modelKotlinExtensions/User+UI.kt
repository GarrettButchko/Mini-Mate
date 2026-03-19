package com.garrettbutchko.minimate.modelKotlinExtensions

import androidx.compose.ui.graphics.Color
import com.garrettbutchko.minimate.datamodels.User
import com.garrettbutchko.minimate.datamodels.UserDTO

val UserDTO.ballColor: Color
    get() = ballColorDT.toColor()

val User.ballColor: Color
    get() = ballColorDT.toColor()

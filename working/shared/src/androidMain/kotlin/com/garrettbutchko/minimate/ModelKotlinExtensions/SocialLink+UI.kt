package com.garrettbutchko.minimate.ModelKotlinExtensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.garrettbutchko.minimate.datamodels.SocialLink
import com.garrettbutchko.minimate.datamodels.SocialPlatform

@Composable
fun SocialLink.platformPainter(): Painter {
    return when (platform) {
        SocialPlatform.INSTAGRAM -> painterResource(id = getDrawableId("instagram"))
        SocialPlatform.FACEBOOK -> painterResource(id = getDrawableId("facebook"))
        SocialPlatform.TIKTOK -> painterResource(id = getDrawableId("tiktok"))
        SocialPlatform.YOUTUBE -> painterResource(id = getDrawableId("youtube"))
        SocialPlatform.WEBSITE -> rememberVectorPainter(Icons.Default.Public)
    }
}

/**
 * Helper to get drawable resource ID by name. 
 * Note: You'll need to make sure these drawables exist in your Android app's res/drawable folder.
 */
@Composable
private fun getDrawableId(name: String): Int {
    val context = LocalContext.current
    val resourceId = context.resources.getIdentifier(name, "drawable", context.packageName)
    return if (resourceId != 0) resourceId else context.resources.getIdentifier("globe", "drawable", context.packageName)
}

package com.garrettbutchko.minimate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.garrettbutchko.minimate.views.App
import com.garrettbutchko.minimate.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (GlobalContext.getOrNull() == null) {
            initKoin {
                androidContext(applicationContext)
                androidLogger()
            }
        }


        setContent {
            App()
        }
    }
}
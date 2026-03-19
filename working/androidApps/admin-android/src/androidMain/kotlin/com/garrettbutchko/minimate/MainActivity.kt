package com.garrettbutchko.minimate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.garrettbutchko.minimate.Views.App
import com.garrettbutchko.minimate.di.initKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initKoin {
            androidContext(this@MainApplication)
            androidLogger()
        }

        setContent {
            App()
        }
    }
}
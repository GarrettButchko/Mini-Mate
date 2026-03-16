import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    val iosTargets = listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

    iosTargets.forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }
    
    jvm()
    
    sourceSets {
        commonMain.dependencies {

            val room_version = "2.8.4"

            implementation("androidx.room:room-runtime:$room_version")
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            
            // Firebase Multiplatform
            implementation(libs.firebase.common)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.storage)
        }

        val androidMain by getting {
            dependencies {
                implementation("com.google.android.libraries.places:places:4.1.0")
                implementation("com.google.maps.android:maps-compose:6.12.0")
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
            }
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.garrettbutchko.minimate.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    buildFeatures {
        compose = true
    }
}

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {

        commonMain.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation("co.touchlab:kermit:2.0.2")
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Room SQLite
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)

            // Firebase Multiplatform
            implementation(libs.firebase.common)
            implementation(libs.firebase.firestore)
            implementation(libs.firebase.auth)
            implementation(libs.firebase.storage)
            implementation(libs.firebase.database)

            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)


            api(project.dependencies.platform(libs.koin.bom))
            api("io.insert-koin:koin-core")
        }

        androidMain.dependencies {
            implementation("com.google.android.libraries.places:places:4.1.0")
            implementation("com.google.maps.android:maps-compose:6.12.0")
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.activity.compose)
            api("io.insert-koin:koin-android")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.io.insert.koin.koin.test)
        }
    }
}

android {
    namespace = "com.garrettbutchko.minimate.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    "kspCommonMainMetadata"(libs.koin.compiler)
}

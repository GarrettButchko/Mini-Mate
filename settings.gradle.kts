rootProject.name = "MiniMate"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":shared", ":shared-admin", ":shared-user", ":admin-android", ":user-android")

project(":shared").projectDir = file("working/shared")
project(":shared-admin").projectDir = file("working/shared-admin")
project(":shared-user").projectDir = file("working/shared-user")
project(":admin-android").projectDir = file("working/androidApps/admin-android")
project(":user-android").projectDir = file("working/androidApps/user-android")

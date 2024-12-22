rootProject.name = "Lumina"
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

dependencyResolutionManagement {
    repositories {
        maven("https://jogamp.org/deployment/maven")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // Add the snapshot repository
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
            content {
                // Optionally restrict this repository to specific groups if needed
                includeGroupByRegex(".*")
            }
        }
    }
}

include(":composeApp")
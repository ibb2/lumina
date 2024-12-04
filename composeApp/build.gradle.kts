import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting
        val version = "0.4.1"


        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
            implementation(libs.android.driver)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation("org.eclipse.angus:angus-mail:2.0.3")
            implementation("com.russhwolf:multiplatform-settings-no-arg:1.2.0")
            implementation("com.russhwolf:multiplatform-settings-make-observable:1.2.0")
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.runtime)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            api("io.github.kevinnzou:compose-webview-multiplatform:1.8.4")
            implementation("app.cash.sqldelight:primitive-adapters:2.0.2")
            implementation("com.composables:core:1.19.1")
//            implementation("com.squareup.sqldelight:coroutines-extensions:2.0.2")
//            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
            implementation("dev.gitlive:firebase-auth:2.1.0")
            implementation("io.ktor:ktor-server-core:3.0.1")
            implementation("io.ktor:ktor-server-netty:3.0.1")
            implementation("io.ktor:ktor-client-content-negotiation:3.0.1")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.jvm.driver)
            implementation(libs.ktor.client.okhttp)
            implementation("net.java.dev.jna:jna:5.15.0")
            implementation("net.java.dev.jna:jna-platform:5.15.0")
            implementation("com.microsoft:credential-secure-storage:1.0.3")
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.native.driver)
        }

    }
}

android {
    namespace = "org.example.project"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.example.project"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.example.project.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "org.example.project"
            packageVersion = "1.0.0"
        }

        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED") // recommended but not necessary

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }

        buildTypes.release.proguard {
            configurationFiles.from("../compose-desktop.pro")
        }

    }
}

sqldelight {
    databases {
        create("LuminaDatabase") {
            packageName.set("com.example.project.database")
            dialect("app.cash.sqldelight:sqlite-3-24-dialect:2.0.2")
        }
    }
}

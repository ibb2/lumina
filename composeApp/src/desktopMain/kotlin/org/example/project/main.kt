package org.example.project

import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.project.database.LuminaDatabase
import dev.datlag.kcef.KCEF
import io.ktor.client.engine.okhttp.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.networking.FirebaseAuthClient
import org.example.project.networking.createHttpClient
import org.example.project.sqldelight.DatabaseDriverFactory
import org.jetbrains.skiko.OS
import java.io.File
import javax.xml.crypto.Data
import kotlin.math.max

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Lumina",
    ) {
        var restartRequired by remember { mutableStateOf(false) }
        var downloading by remember { mutableStateOf(0F) }
        var initialized by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                KCEF.init(builder = {
                    release("jbr-release-17.0.10b1087.23")
//                    release("jbr_jcef-17.0.10-windows-x64-b1087.23")
                    installDir(File("kcef-bundle"))
                    progress {
                        onDownloading {
                            downloading = max(it, 0F)
                        }
                        onInitialized {
                            initialized = true
                        }
                    }
                    settings {
                        cachePath = File("cache").absolutePath
                    }
                }, onError = {
                    it!!.printStackTrace()
                }, onRestartRequired = {
                    restartRequired = true
                })
            }
        }

        if (restartRequired) {
            Text(text = "Restart required.")
        } else {
            if (initialized) {
                App(client = FirebaseAuthClient(httpClient = createHttpClient(OkHttp.create())), emailService = EmailService(FirebaseAuthClient(httpClient = createHttpClient(OkHttp.create()))), authentication = Authentication(), driver = DatabaseDriverFactory().create())
            } else {
                Text(text = "Downloading $downloading%")
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                KCEF.disposeBlocking()
            }
        }

    }
}
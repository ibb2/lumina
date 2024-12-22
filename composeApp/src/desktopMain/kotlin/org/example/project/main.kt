package org.example.project

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.project.database.LuminaDatabase
import com.konyaco.fluent.Background
import com.konyaco.fluent.background.Mica
import com.konyaco.fluent.component.rememberNavigationState
import com.mayakapps.compose.windowstyler.WindowBackdrop
import com.mayakapps.compose.windowstyler.WindowCornerPreference
import com.mayakapps.compose.windowstyler.WindowFrameStyle
import com.mayakapps.compose.windowstyler.WindowStyle
import dev.datlag.kcef.KCEF
import io.ktor.client.engine.okhttp.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import lumina.composeapp.generated.resources.Res
import org.example.project.networking.FirebaseAuthClient
import org.example.project.networking.createHttpClient
import org.example.project.sqldelight.DatabaseDriverFactory
import org.example.project.ui.window.WindowFrame
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import java.io.File
import javax.xml.crypto.Data
import kotlin.math.max

fun main() = application {

    val state = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(1280.dp, 720.dp)
    )
    val title = "Lumina"

    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = title,
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
//                WindowFrame(
//                    onCloseRequest = ::exitApplication,
//                    title = title,
//                    state = state,
//                    backButtonEnabled = false,
//                    backButtonClick = { fun foo() {}  },
//                    backButtonVisible = hostOs.isWindows
//                ) { _, _ ->
                    WindowStyle(
                        isDarkTheme = isSystemInDarkTheme(),
                        backdropType = WindowBackdrop.Mica,
                        frameStyle = WindowFrameStyle(cornerPreference = WindowCornerPreference.ROUNDED),
                    )
                    App(
                        client = FirebaseAuthClient(httpClient = createHttpClient(OkHttp.create())),
                        emailService = EmailService(FirebaseAuthClient(httpClient = createHttpClient(OkHttp.create()))),
                        authentication = Authentication(),
                        driver = DatabaseDriverFactory().create()
                    )
//                }
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
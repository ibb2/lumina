package org.example.project

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import cafe.adriel.voyager.navigator.Navigator
import com.konyaco.fluent.gallery.window.WindowFrame
import dev.datlag.kcef.KCEF
import io.ktor.client.engine.okhttp.*
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.networking.FirebaseAuthClient
import org.example.project.networking.createHttpClient
import org.example.project.sqldelight.DatabaseDriverFactory
import org.jetbrains.skiko.hostOs

fun main() = application {
    val state = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(1280.dp, 720.dp)
    )
    val title = "Lumina"

    // Create a state to track navigation
    var navigator by remember { mutableStateOf<Navigator?>(null) }
    var canGoBack by remember { mutableStateOf(false) }

    LaunchedEffect(navigator) {
        snapshotFlow { navigator?.canPop ?: false }
            .collect { canPop -> canGoBack = canPop }
    }

    Window(onCloseRequest = ::exitApplication, state = state, title = title) {
        var restartRequired by remember { mutableStateOf(false) }
        var downloading by remember { mutableStateOf(0F) }
        var initialized by remember { mutableStateOf(false) }

        // Initialize KCEF before UI rendering
        LaunchedEffect(Unit) {
            try {
                withContext(Dispatchers.IO) {
                    KCEF.init(builder = {
                        installDir(File("kcef-bundle"))
                        progress {
                            onDownloading { downloading = max(it, 0F) }
                            onInitialized { initialized = true }
                        }
                        // Instead of the DSL block, call release directly:
                        settings {
                            cachePath = File("cache").absolutePath
                        }
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
                restartRequired = true
            }
        }

        WindowFrame(
            onCloseRequest = ::exitApplication,
            title = title,
            state = state,
            backButtonEnabled = canGoBack,
            backButtonClick = { navigator?.pop() },
            backButtonVisible = hostOs.isWindows
        ) { windowInset, contentInset ->
            when {
                restartRequired -> {
                    Text("Restart required.")
                }

                !initialized -> {
                    Box(Modifier.fillMaxSize()) {
                        Text(
                            "Downloading $downloading%",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                else -> {
                    val colors = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.else LightColorPalette
                            MaterialTheme(colorScheme = colors) {
                                App(
                                    client = FirebaseAuthClient(
                                        httpClient = createHttpClient(OkHttp.create())
                                    ),
                                    emailService = EmailService(
                                        FirebaseAuthClient(
                                            httpClient = createHttpClient(OkHttp.create())
                                        )
                                    ),
                                    authentication = Authentication(),
                                    driver = DatabaseDriverFactory().create(),
                                    windowInset = windowInset,
                                    contentInset = contentInset,
                                    onNavigatorReady = { nav -> navigator = nav }
                                )
                            }
                }
            }
        }

        DisposableEffect(Unit) { onDispose { KCEF.disposeBlocking() } }
    }
}

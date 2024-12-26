package org.example.project.ui.platformSpecific.emails

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
actual fun emailsDialog(
    visible: Boolean,
    emailFromUser: String,
    emailSubject: String,
    emailContent: String,
    onDismiss: () -> Unit
) {
    if (visible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties =
                DialogProperties(
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true,
                    usePlatformDefaultWidth =
                        false // This allows us to control the width
                )
        ) {
            Surface(
                modifier =
                    Modifier.fillMaxWidth(
                        0.60f
                    ) // Increased width to better fit email content
                        .fillMaxHeight(0.80f),
                shape = MaterialTheme.shapes.medium,
                elevation = 24.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(backgroundColor = MaterialTheme.colors.surface, elevation = 0.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Email Details", style = MaterialTheme.typography.h6)
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    // Force recomposition to clear any lingering content
//                                    rememberCoroutineScope1().launch { delay(100) }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Close"
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                            Text(
                                text = "From: $emailFromUser",
                                style = MaterialTheme.typography.subtitle1,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Subject: $emailSubject",
                                style = MaterialTheme.typography.h6,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Divider()
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            val state = rememberWebViewStateWithHTMLData(emailContent)

                            val loadingState = state.loadingState
                            if (loadingState is LoadingState.Loading) {
                                LinearProgressIndicator(
                                    progress = loadingState.progress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            WebView(
                                state = state,
                                modifier = Modifier.fillMaxSize(),
                                captureBackPresses =
                                    false // Prevents WebView from capturing back presses
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                // Force recomposition to clear any lingering content
//                                rememberCoroutineScope().launch { delay(100) }
                            }
                        ) { Text("Close") }
                    }
                }
            }
        }
    }
}

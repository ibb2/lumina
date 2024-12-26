package org.example.project.ui.platformSpecific.emails

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.composables.core.rememberScrollAreaState
import com.konyaco.fluent.component.DialogSize
import com.konyaco.fluent.component.FluentDialog
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import org.example.project.shared.data.EmailsDAO
import org.example.project.ui.platformSpecific.PlatformSpecificCard

@Composable
actual fun emailsDialog(
    visible: Boolean,
    emailFromUser: String,
    emailSubject: String,
    emailContent: String,
    onDismiss: () -> Unit
) {
    if (visible) {
        DialogWindow(
            onCloseRequest = onDismiss,
            title = "Email Details",
            state = rememberDialogState(width = 800.dp, height = 600.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Email header section
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

                // Email content section
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val state = rememberWebViewStateWithHTMLData(emailContent)

                    // Loading indicator
                    val loadingState = state.loadingState
                    if (loadingState is LoadingState.Loading) {
                        LinearProgressIndicator(
                            progress = loadingState.progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // WebView for email content
                    WebView(state = state, modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

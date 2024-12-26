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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
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
actual fun emailsDialog(visible: Boolean, emailFromUser: String, emailSubject: String, emailContent: String) {

    // Send email
    var sendEmail by remember { mutableStateOf(false) }


    val state = rememberWebViewStateWithHTMLData(emailContent)
    LaunchedEffect(Unit) {
        state.webSettings.apply {
            logSeverity = KLogSeverity.Debug
            customUserAgentString =
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1) AppleWebKit/625.20 (KHTML, like Gecko) Version/14.3.43 Safari/625.20"
        }
    }
    val navigator = rememberWebViewNavigator()
    var textFieldValue by remember(state.lastLoadedUrl) {
        mutableStateOf(state.lastLoadedUrl)
    }

    val loadingState = state.loadingState
    if (loadingState is LoadingState.Loading) {
        LinearProgressIndicator(
            progress = loadingState.progress,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // Vertical scrollbar
    val lazyListState = rememberLazyListState()
    val scrollState = rememberScrollAreaState(lazyListState)

    // Modify the list to remove emails from logged-out accounts
//    LaunchedEffect(accounts) {
//        // Create a set of valid account emails
//        val validAccountEmails = accounts.map { it.email }.toSet()
//
//        // Remove emails that don't belong to current accounts
//        emails.removeAll { email ->
//            email.account !in validAccountEmails
//        }
//
//        // Similarly, remove orphaned attachments
//        attachments.removeAll { attachment ->
//            // Remove attachments for emails that are no longer in the list
//            emails.none { it.id == attachment.emailId }
//        }
//    }

    FluentDialog(
        visible,
        size = DialogSize.Max,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
    ) {
        // Draw a rectangle shape with rounded corners inside the dialog
        PlatformSpecificCard {
            Column {
                TopAppBar(
                    title = { Text(text = "WebView Sample") },
                    navigationIcon = {
                        if (navigator.canGoBack) {
                            IconButton(onClick = { navigator.navigateBack() }) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                )
                            }
                        }
                    },
                )

                Row {
                    Box(modifier = Modifier.weight(1f)) {
                        if (state.errorsForCurrentRequest.isNotEmpty()) {
                            Image(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Error",
                                colorFilter = ColorFilter.tint(Color.Red),
                                modifier =
                                    Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(8.dp),
                            )
                        }

                        OutlinedTextField(
                            value = textFieldValue ?: "",
                            onValueChange = { textFieldValue = it },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Button(
                        onClick = {
                            textFieldValue?.let {
                                navigator.loadUrl(it)
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterVertically),
                    ) {
                        Text("Go")
                    }
                }
            }

            WebView(
                state = state,
                modifier =
                    Modifier
                        .fillMaxSize(),
                navigator = navigator,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Text(
                    text = "This is a dialog with buttons and an image.",
                    modifier = Modifier.padding(16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { /* TODO */ },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Dismiss")
                    }
                    TextButton(
                        onClick = { /* TODO*/ },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}
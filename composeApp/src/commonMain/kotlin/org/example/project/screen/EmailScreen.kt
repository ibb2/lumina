package org.example.project.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.db.SqlDriver
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import org.example.project.shared.data.EmailsDAO
import org.example.project.EmailService
import org.example.project.Authentication


data class EmailScreen(
    val email: EmailsDAO,
    val index: Number,
    val emailService: EmailService,
    val authentication : Authentication,
    val driver : SqlDriver,
) : Screen {

    @Composable
    override fun Content() {
        DisplayEmail(
            email = email,
            index = index,
            emailService = emailService,
            authentication = authentication,
            driver = driver,
        )

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayEmail(
    email: EmailsDAO,
    index: Number,
    emailService: EmailService,
    authentication: Authentication,
    driver: SqlDriver,
) {

    val localNavigator = LocalNavigator.currentOrThrow
    var display: Boolean by remember { mutableStateOf(false) }
    var emailFromUser: String by remember { mutableStateOf("") }
    var emailSubject: String by remember { mutableStateOf("") }
    var emailContent: String by remember { mutableStateOf(email.body) }

    // Send email
    var sendEmail by remember { mutableStateOf(false) }

    fun displayEmailBody(show: Boolean, email: EmailsDAO) {

        emailFromUser = ""
        emailSubject = ""
        emailContent = ""

        if (show) {
            display = true
            emailFromUser = email.senderAddress
            emailSubject = email.subject
            emailContent = email.body
        }
    }

    val state = rememberWebViewStateWithHTMLData(emailContent)
    LaunchedEffect(Unit) {
        state.webSettings.apply {
            logSeverity = KLogSeverity.Debug
            customUserAgentString =
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1) AppleWebKit/625.20 (KHTML, like Gecko) Version/14.3.43 Safari/625.20"
        }
    }
    val navigator = rememberWebViewNavigator()
    var textFieldValue by remember(state.lastLoadedUrl) { mutableStateOf(state.lastLoadedUrl) }

    val loadingState = state.loadingState
    if (loadingState is LoadingState.Loading) {
        LinearProgressIndicator(
            progress = loadingState.progress,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Card {
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            IconButton(
                onClick = {
                    localNavigator.pop()
                }
            ) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Go back to home screen") }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = email.senderAddress)
            Text(text = email.account)
            Text(text = email.receivedDate)
        }
        if (email.recipients.isNotEmpty()) {
            // TODO Handle Bcc and Ccc
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
}
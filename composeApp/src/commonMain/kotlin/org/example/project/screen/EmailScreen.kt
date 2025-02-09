package org.example.project.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val authentication: Authentication,
    val driver: SqlDriver,
) : Screen {

    @Composable
    override fun Content() {
        Spacer(modifier = Modifier.height(48.dp))
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
    var emailContent: String by remember { mutableStateOf(if (email.htmlBody.isNullOrEmpty()) email.body else email.htmlBody) }

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


    Column(
        modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            contentAlignment = Alignment.TopStart
        ) {
            IconButton(
                onClick = {
                    localNavigator.pop()
                }
            ) { Icon(Icons.Rounded.ChevronLeft, contentDescription = "Go back to home screen") }
        }

        Card(shape = RoundedCornerShape(20)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.fillMaxWidth(0.5f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Text(text = email.senderAddress)
                    Text(text = email.account)
                }
                Row(modifier = Modifier.fillMaxWidth(0.5f), horizontalArrangement = Arrangement.End) {
                    Text(text = email.receivedDate)
                }
            }
        }
//        Card {
//            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//                if (email.recipients.isNotEmpty()) {
//                    // TODO Handle Bcc and Ccc
//                }
//
//            }
//        }

        Card() {
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
                    modifier = Modifier.fillMaxSize().padding(16.dp).clip(shape = RoundedCornerShape(30)),
                    captureBackPresses =
                        false // Prevents WebView from capturing back presses
                )
            }
        }
    }
}
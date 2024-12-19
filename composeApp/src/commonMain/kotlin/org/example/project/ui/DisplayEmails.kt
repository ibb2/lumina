package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.core.ScrollArea
import com.composables.core.Thumb
import com.composables.core.VerticalScrollbar
import com.composables.core.rememberScrollAreaState
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.*
import org.example.project.EmailService
import org.example.project.data.NewEmail
import org.example.project.deleteEmail
import org.example.project.read
import org.example.project.shared.data.AccountsDAO
import org.example.project.shared.data.AttachmentsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.EmailsDataSource
import org.example.project.ui.platformSpecific.PlatformSpecificCard
import org.example.project.ui.platformSpecific.PlatformSpecificDelete

@Composable
fun displayEmails(
    accounts: List<AccountsDAO>,
    selectedFolders: MutableState<List<String>>,
    emails: MutableList<EmailsDAO>,
    attachments: MutableList<AttachmentsDAO>,
    emailDataSource: EmailsDataSource,
    emailService: EmailService
) {
    var display: Boolean by remember { mutableStateOf(false) }
    var emailFromUser: String by remember { mutableStateOf("") }
    var emailSubject: String by remember { mutableStateOf("") }
    var emailContent: String by remember { mutableStateOf("") }

    // Send email
    var sendEmail by remember { mutableStateOf(false) }


    fun displayEmailBody(show: Boolean, email: EmailsDAO) {

        emailFromUser = ""
        emailSubject = ""
        emailContent = ""

        if (show) {
            display = true
            emailFromUser = email.sender ?: ""
            emailSubject = email.subject ?: ""
            emailContent = email.body ?: ""
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
    LaunchedEffect(accounts) {
        // Create a set of valid account emails
        val validAccountEmails = accounts.map { it.email }.toSet()

        // Remove emails that don't belong to current accounts
        emails.removeAll { email ->
            email.account !in validAccountEmails
        }

        // Similarly, remove orphaned attachments
        attachments.removeAll { attachment ->
            // Remove attachments for emails that are no longer in the list
            emails.none { it.id == attachment.emailId }
        }
    }

    val allEmails = remember(selectedFolders.value, emails) {
        if (selectedFolders.value.isNotEmpty()) {
            emails.filter { email -> email.folderName in selectedFolders.value }
        } else {
            emails
        }
    }



    Column(verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight(0.7f)) {
        // Email
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Button(
                onClick = {
                    sendEmail = true
                }) {
                Text(text = "Send Email")
            }
        }
        ScrollArea(state = scrollState) {
            Column(modifier = Modifier.fillMaxWidth()) {

                LazyColumn(state = lazyListState) {
                    itemsIndexed(allEmails) { index, email ->

                        val emailAddress = accounts.find { it.email == email.account }?.email ?: "Unknown Account"
                        var isRead by remember { mutableStateOf(email.isRead) }
                        println("Emails ${email.subject}")
                        PlatformSpecificCard(Modifier) {
                            Text(
                                text = email.sender,
                            )
                            Text(
                                text = email.subject
                            )
                            Button(
                                onClick = {
                                    displayEmailBody(!display, email)
                                },
                            ) {
                                Text("View Email")
                            }
                            Button(
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        isRead =
                                            read(email, emailDataSource, emailService, emailAddress)
                                                ?: false
                                    }
                                }
                            ) {
                                Text(text = if (isRead) "Mark as unread" else "Mark as read")
                            }
                            PlatformSpecificDelete(Modifier, onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    deleteEmail(email, emailDataSource, emailService, emailAddress)
                                    // Remove email on the main thread
                                    withContext(Dispatchers.Main) {
                                        emails.remove(email)
                                    }
                                }
                            })
                            if (attachments.any { it.emailId === email.id }) {
                                Row {
                                    attachments.filter { it.emailId === email.id }.forEach { attachment ->
                                        Row {
                                            Text(
                                                text = attachment.fileName,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                            )
                                            Text(
                                                text = attachment.size.toString(), modifier = Modifier
                                                    .padding(8.dp)

                                            )
                                            Text(
                                                text = attachment.mimeType, modifier = Modifier
                                                    .padding(8.dp)
                                            )
                                        }

                                    }
                                }
                            }

                        }
//                        Column(
//                            modifier = Modifier.border(
//                                width = 1.dp,
//                                color = Color.DarkGray,
//                                shape = RoundedCornerShape(4.dp)
//                            ).background(
//                                color = Color.LightGray
//                            ).fillMaxWidth()
//                        ) {
//                            Text(text = "Account $emailAddress", color = Color.hsl(38f, 0.5f, 0.5f))
//                            Text(
//                                text = email.sender,
//                            )
//                            Text(
//                                text = email.subject
//                            )
//                            Button(
//                                onClick = {
//                                    displayEmailBody(!display, email)
//                                },
//                            ) {
//                                Text("View Email")
//                            }
//                            Text("Email read: $isRead")
//                            Button(
//                                onClick = {
//                                    CoroutineScope(Dispatchers.IO).launch {
//                                        isRead =
//                                            read(email, emailDataSource, emailService, emailAddress)
//                                                ?: false
//                                    }
//                                }
//                            ) {
//                                Text(text = if (isRead) "Mark as unread" else "Mark as read")
//                            }
//                            Button(
//                                onClick = {
//                                    CoroutineScope(Dispatchers.IO).launch {
//                                        deleteEmail(email, emailDataSource, emailService, emailAddress)
//                                        // Remove email on the main thread
//                                        withContext(Dispatchers.Main) {
//                                            emails.remove(email)
//                                        }
//                                    }
//                                },
//                                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
//                            ) {
//                                Text(text = "Delete")
//                            }
//                            if (attachments.any { it.emailId === email.id }) {
//                                Row {
//                                    attachments.filter { it.emailId === email.id }.forEach { attachment ->
//                                        Row {
//                                            Text(
//                                                text = attachment.fileName,
//                                                modifier = Modifier
//                                                    .padding(8.dp)
//                                            )
//                                            Text(
//                                                text = attachment.size.toString(), modifier = Modifier
//                                                    .padding(8.dp)
//
//                                            )
//                                            Text(
//                                                text = attachment.mimeType, modifier = Modifier
//                                                    .padding(8.dp)
//                                            )
//                                        }
//
//                                    }
//                                }
//                            } else {
//                                Text(
//                                    text = "No attachments",
//                                )
//                            }
//                        }
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().width(12.dp)
            ) {
                Thumb(Modifier.background(Color.Black))
            }
        }
    }


//

    if (display) {
        Dialog(
            onDismissRequest = { display = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            ),
        ) {
            // Draw a rectangle shape with rounded corners inside the dialog
            Surface(modifier = Modifier.fillMaxSize(0.6f)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(525.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
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
                                        imageVector = Icons.Default.Close,
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
    }

    if (sendEmail) {
        var sendEmailFrom by remember { mutableStateOf("") }
        var sendEmailTo by remember { mutableStateOf("") }
        var sendEmailSubject by remember { mutableStateOf("") }
        var sendEmailBody by remember { mutableStateOf("") }

        fun sendEmail() {
            println("Sending email... $sendEmailFrom, $sendEmailTo, $sendEmailSubject, $sendEmailBody")
            var sentEmailSuccess = false
            CoroutineScope(Dispatchers.IO).launch {
                sentEmailSuccess = emailService.sendNewEmail(
                    emailDataSource,
                    NewEmail(
                        from = sendEmailFrom,
                        to = sendEmailTo,
                        subject = sendEmailSubject,
                        body = sendEmailBody
                    ),
                    sendEmailFrom
                )
            }

            println("Email sent successfully? $sentEmailSuccess")

            sendEmail = false
        }

        Dialog(
            onDismissRequest = { sendEmail = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            ),
        ) {
            // Draw a rectangle shape with rounded corners inside the dialog
            Surface(modifier = Modifier.fillMaxSize(0.9f)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(525.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TextField(
                            label = { Text("From") },
                            value = sendEmailFrom,
                            onValueChange = { sendEmailFrom = it },
                            modifier = Modifier.padding(16.dp),
                        )
                        TextField(
                            label = { Text("To") },
                            value = sendEmailTo,
                            onValueChange = { sendEmailTo = it },
                            modifier = Modifier.padding(16.dp)
                        )
                        TextField(
                            label = { Text("Subject") },
                            value = sendEmailSubject,
                            onValueChange = { sendEmailSubject = it },
                            modifier = Modifier.padding(16.dp)
                        )
                        TextField(
                            label = { Text("Body") },
                            value = sendEmailBody,
                            onValueChange = { sendEmailBody = it },
                            modifier = Modifier.padding(16.dp),
                        )
                        Button(onClick = {
                            sendEmail()
                        }) {
                            Text(text = "Send")
                        }
                    }
                }
            }
        }
    }
}
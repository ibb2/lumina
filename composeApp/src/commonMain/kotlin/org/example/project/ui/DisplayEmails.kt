package org.example.project.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.cash.sqldelight.db.SqlDriver
import cafe.adriel.voyager.navigator.Navigator
import com.composables.core.*
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.*
import org.example.project.Authentication
import org.example.project.EmailService
import org.example.project.data.NewEmail
import org.example.project.deleteEmail
import org.example.project.networking.FirebaseAuthClient
import org.example.project.read
import org.example.project.shared.data.AccountsDAO
import org.example.project.shared.data.AttachmentsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.AccountsDataSource
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.EmailsDataSource
import org.example.project.ui.platformSpecific.*
import org.example.project.ui.platformSpecific.emails.emailsDialog
import org.example.project.ui.platformSpecific.emails.sendEmail

@Composable
fun displayEmails(
    accounts: List<AccountsDAO>,
    selectedFolders: MutableState<List<String>>,
    emails: MutableList<EmailsDAO>,
    attachments: MutableList<AttachmentsDAO>,
    emailDataSource: EmailsDataSource,
    emailService: EmailService,
    localNavigator: Navigator,
    authentication: Authentication,
    client: FirebaseAuthClient,
    driver: SqlDriver,
    accountsDataSource: AccountsDataSource,
    attachmentsDataSource: AttachmentsDataSource,
) {

    //    val localNavigator = LocalNavigator.currentOrThrow

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

    // Vertical scrollbar
    val lazyListState = rememberLazyListState()
    val scrollState = rememberScrollAreaState(lazyListState)

    // Modify the list to remove emails from logged-out accounts
    LaunchedEffect(accounts) {
        // Create a set of valid account emails
        val validAccountEmails = accounts.map { it.email }.toSet()

        // Remove emails that don't belong to current accounts
        emails.removeAll { email -> email.account !in validAccountEmails }

        // Similarly, remove orphaned attachments
        attachments.removeAll { attachment ->
            // Remove attachments for emails that are no longer in the list
            emails.none { it.id == attachment.emailId }
        }
    }

    val allEmails =
        remember(selectedFolders.value, emails) {
            if (selectedFolders.value.isNotEmpty()) {
                emails.filter { email -> email.folderName in selectedFolders.value }
            } else {
                emails
            }
        }

    Box(contentAlignment = Alignment.BottomEnd) {
        Row(
            modifier = Modifier.padding(end = 16.dp).zIndex(10f),
            horizontalArrangement = Arrangement.End
        ) { Button(onClick = { sendEmail = true }) { Text(text = "Send Email") } }
        Row(verticalAlignment = Alignment.Bottom) {
            ScrollArea(state = scrollState) {
                LazyColumn(
                    state = lazyListState,
                ) {
                    itemsIndexed(allEmails) { index, email ->
                        val emailAddress =
                            accounts.find { it.email == email.account }?.email
                                ?: "Unknown Account"
                        var isRead by remember { mutableStateOf(email.isRead) }
                        println("Emails ${email.subject}")

                        val displayEmail = { displayEmailBody(!display, email) }
                        val markAsRead = {
                            CoroutineScope(Dispatchers.IO).launch {
                                isRead =
                                    read(email, emailDataSource, emailService, emailAddress)
                                        ?: false
                            }
                        }
                            Column(
                                modifier = Modifier.fillMaxWidth().border(
                                    shape = RectangleShape,
                                    width = 0.dp,
                                    brush = Brush.horizontalGradient(colors = listOf(Color.Black, Color.Black)),
                                )
                            ) {
                                Text(
                                    text = "${email.senderAddress} |_> $emailAddress"
                                )
                                Text(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    text =
                                        if (email.subject.length > 60) {
                                            email.subject.substring(0, 50) + "..."
                                        } else {
                                            email.subject
                                        }
                                )
                            }
////                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
////                                PlatformSpecificMarkAsRead(
////                                    Modifier,
////                                    isRead,
////                                    {
////                                        CoroutineScope(Dispatchers.IO).launch {
////                                            isRead =
////                                                read(
////                                                    email,
////                                                    emailDataSource,
////                                                    emailService,
////                                                    emailAddress
////                                                )
////                                                    ?: false
////                                        }
////                                    }
////                                )
////                                PlatformSpecificDelete(
////                                    Modifier,
////                                    onClick = {
////                                        CoroutineScope(Dispatchers.IO).launch {
////                                            deleteEmail(
////                                                email,
////                                                emailDataSource,
////                                                emailService,
////                                                emailAddress
////                                            )
////                                            // Remove email on the main thread
////                                            withContext(Dispatchers.Main) {
////                                                emails.remove(email)
////                                            }
////                                        }
////                                    }
////                                )
////                            }
////                            if (attachments.any { it.emailId === email.id }) {
////                                Row {
////                                    attachments.filter { it.emailId === email.id }.forEach { attachment ->
////                                        Row {
////                                            Text(
////                                                text = attachment.fileName,
////                                                modifier = Modifier.padding(8.dp)
////                                            )
////                                            Text(
////                                                text = attachment.size.toString(),
////                                                modifier = Modifier.padding(8.dp)
////                                            )
////                                            Text(
////                                                text = attachment.mimeType,
////                                                modifier = Modifier.padding(8.dp)
////                                            )
////                                        }
////                                    }
////                                }
////                            }
                    }
                }
                VerticalScrollbar(modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight()) {
                    Thumb(
                        modifier =
                            Modifier.background(
                                Color.Black.copy(0.3f),
                                RoundedCornerShape(100)
                            ),
                        thumbVisibility =
                            ThumbVisibility.HideWhileIdle(
                                enter = fadeIn(),
                                exit = fadeOut(),
                                hideDelay = 1.seconds
                            )
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.padding(vertical = 32.dp)) {
        emailsDialog(display, emailFromUser, emailSubject, emailContent) { display = false }
    }


    fun sendEmailAction(from: String, to: String, subject: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val sentEmailSuccess =
                emailService.sendNewEmail(
                    emailDataSource,
                    NewEmail(
                        from = from,
                        to = to,
                        subject = subject,
                        body = body
                    ),
                    from
                )
            println("Email sent successfully? $sentEmailSuccess")
        }
    }


    if (sendEmail) {
        sendEmail(
            emailService = emailService,
            emailDataSource = emailDataSource,
            onClose = { sendEmail = false },
            onSend = { from, to, subject, body ->
                sendEmailAction(from, to, subject, body = body)
            }
        )
    }
}


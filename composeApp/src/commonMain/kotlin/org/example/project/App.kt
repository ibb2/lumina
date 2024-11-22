package org.example.project

import com.composables.core.VerticalScrollbar
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.example.AccountsTableQueries
import com.example.EmailsTableQueries
import com.example.Emails
import com.example.project.database.LuminaDatabase
import com.multiplatform.webview.util.KLogSeverity
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.observable.makeObservable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import org.example.project.shared.data.AttachmentsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.EmailsDataSource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import com.composables.core.ScrollArea
import com.composables.core.Thumb
import com.composables.core.rememberScrollAreaState
import org.example.project.shared.utils.createCompositeKey


@OptIn(ExperimentalSettingsApi::class)
@Composable
@Preview
fun App(emailService: EmailService, driver: SqlDriver) {
    MaterialTheme {

        // db related stuff
        val database = LuminaDatabase(
            driver,
            EmailsAdapter = Emails.Adapter(
                attachments_countAdapter = IntColumnAdapter
            ),
        )

        // Queries
        val accountQueries = database.accountsTableQueries
        val emailQueries = database.emailsTableQueries

        // Data Sources
        val emailDataSource: EmailsDataSource = EmailsDataSource(database)
        val attachmentsDataSource: AttachmentsDataSource = AttachmentsDataSource(database)

        // Basic Auth
        val settings = Settings()
        var showContent by remember { mutableStateOf(false) }
        var emailAddress by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var loggedIn by remember { mutableStateOf(false) }

        val emailCount by emailService.getEmailCount(emailDataSource).collectAsState()

        val observableSettings: ObservableSettings = settings.makeObservable()

        observableSettings.putString("emailAddress", "")
        observableSettings.putString("password", "")
        observableSettings.putBoolean("login", false)

        observableSettings.addBooleanListener("login", defaultValue = false) { value ->
            loggedIn = value
        }
        observableSettings.addStringListener(
            "emailAddress",
            defaultValue = ""
        ) { value -> emailAddress = value }
        observableSettings.addStringListener("password", defaultValue = "") { value ->
            password = value
        }

        // Ui

        if (!loggedIn) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Text(
                    "Database Email Count: $emailCount"
                )
                Text(
                    "Login",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
                TextField(
                    value = emailAddress,
                    onValueChange = { it -> emailAddress = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                TextField(
                    value = password,
                    onValueChange = { it -> password = it },
                    label = { Text("Password") },
                    modifier = Modifier.padding(bottom = 8.dp)

                )
                Button(onClick = {
                    login(observableSettings, accountQueries, emailAddress, password)
                }) {
                    Text("Login")
                }
                Button(onClick = {
                    GlobalScope.launch(Dispatchers.IO) {
                        emailService.deleteEmails(emailDataSource)
                    }
                }) {
                    Text("Delete Emails")
                }
            }

        } else {
            Button(onClick = { logout(observableSettings) }) {
                Text(
                    "Logout"
                )
            }
            displayEmails(
                emailDataSource,
                observableSettings,
                emailQueries,
                accountQueries,
                emailService,
                loggedIn,
                emailAddress,
                password
            )
        }
    }
}

fun login(
    observableSettings: ObservableSettings,
    accountQueries: AccountsTableQueries,
    emailAddress: String,
    password: String
): Unit {
    observableSettings.putString("emailAddress", emailAddress)
    observableSettings.putString("password", password)
    observableSettings.putBoolean("login", true)

    try {
        accountQueries.selectAccount(emailAddress).executeAsOneOrNull()
            ?: throw NullPointerException()
    } catch (e: NullPointerException) {
        accountQueries.insertAccount(emailAddress)
    }

    println("Logged in as $emailAddress")
}

fun logout(observableSettings: ObservableSettings): Unit {
    observableSettings.putString("emailAddress", "")
    observableSettings.putString("password", "")
    observableSettings.putBoolean("login", false)
}


@Composable
fun displayEmails(
    emailDataSource: EmailsDataSource,
    observableSettings: ObservableSettings,
    emailTableQueries: EmailsTableQueries,
    accountQueries: AccountsTableQueries,
    emailService: EmailService,
    loggedIn: Boolean,
    emailAddress: String,
    password: String
) {
    var display: Boolean by remember { mutableStateOf(false) }
    var emailFromUser: String by remember { mutableStateOf("") }
    var emailSubject: String by remember { mutableStateOf("") }
    var emailContet: String by remember { mutableStateOf("") }

    // Coroutine Scope
    var currentProgress by remember { mutableStateOf(0f) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope() // Create a coroutine scope

    fun displayEmailBody(show: Boolean, email: EmailsDAO) {

        emailFromUser = ""
        emailSubject = ""
        emailContet = ""

        if (show) {
            display = true
            emailFromUser = email.sender ?: ""
            emailSubject = email.subject ?: ""
            emailContet = email.body ?: ""
        }
    }

    val state = rememberWebViewStateWithHTMLData(emailContet)
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

    if (loggedIn && emailAddress.isNotEmpty() && password.isNotEmpty()) {

        var isLoading by remember { mutableStateOf(false) }
        var emails by remember { mutableStateOf<List<EmailsDAO>>(emptyList()) } // Store emails
        var attachments by remember { mutableStateOf<List<AttachmentsDAO>>(emptyList()) } // Store attachments

        val totalEmails by emailService.totalEmails.collectAsState()
        val emailsReadCount by emailService.emailsRead.collectAsState()

        // Vertical scrollbar
        var lazyListState = rememberLazyListState()
        val scrollState = rememberScrollAreaState(lazyListState)

        LaunchedEffect(emailsReadCount) {
            loadProgress(emailsReadCount, totalEmails) { progress ->
                currentProgress = progress
            }
        }

        LaunchedEffect(Unit) { // Trigger once
            isLoading = true
            try {
                // Replace with your actual email retrieval logic

                val startTime = Clock.System.now()
                val returned = withContext(Dispatchers.IO) {
                    emailService.getEmails(
                        emailDataSource,
                        emailTableQueries,
                        accountQueries,
                        emailAddress,
                        password
                    )
                }
                val endTime = Clock.System.now()
                val duration = endTime - startTime
                println("Emails loaded in ${duration.inWholeSeconds} seconds or ${duration.inWholeMilliseconds} ms")

                withContext(Dispatchers.Main) {
                    emails = returned.first
                    attachments = returned.second
                    isLoading = false // Hide loading indicator after updating emails
                }

            } catch (e: Exception) {
                // Handle error, e.g., show an error message
                println("Error in App ${e.message}")
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            } finally {
                isLoading = false
            }
        }

        if (isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Loading emails...")
                Text("Emails read: ${emailsReadCount} out of ${totalEmails}")
                LinearProgressIndicator(
//                    progress = currentProgress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Display email content
            ScrollArea(state = scrollState) {
                LazyColumn(modifier = Modifier.fillMaxWidth(),state = lazyListState) {
                    items(emails) { email ->
                        Column(
                            modifier = Modifier.border(
                                width = 1.dp,
                                color = Color.DarkGray,
                                shape = RoundedCornerShape(4.dp)
                            ).background(
                                color = Color.LightGray
                            ).fillMaxWidth()
                        ) {
                            Text(
                                text = email.sender ?: "No from",
                            )
                            Text(
                                text = email.subject ?: "No subject"
                            )
                            Button(
                                onClick = {
                                    displayEmailBody(!display, email)
                                },
                            ) {
                                Text("View Email")
                            }
//                            Button(
//                                onClick = {
//                                    read()
//                                }
//                            ) {
//                                Text ("Mark as read")
//                            }
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
                            } else {
                                Text(
                                    text = "No attachments",
                                )
                            }
                        }

                    }

                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().width(4.dp)
                ) {
                    Thumb(Modifier.background(Color.Black))
                }
            }
        }
    }

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

}

//suspend fun read(email: EmailsDAO, emailsDataSource: EmailsDataSource) {
//
//    val emailCompKey = createCompositeKey(email.subject, email.receivedDate, email.sender)
//    val emailEntry =emailsDataSource.selectEmail(emailCompKey)
//
//
//
//}

suspend fun loadProgress(emailsRead: Int, totalEmails: Int, updateProgress: (Float) -> Unit) {
    updateProgress(emailsRead.toFloat() / totalEmails)
}


expect class EmailService {

    val emailsRead: StateFlow<Int>
    val totalEmails: StateFlow<Int>
    var emailCount: Int


    suspend fun getEmails(
        emailDataSource: EmailsDataSource,
        emailTableQueries: EmailsTableQueries,
        accountQueries: AccountsTableQueries,
        emailAddress: String,
        password: String
    ): Pair<MutableList<EmailsDAO>, MutableList<AttachmentsDAO>>

    suspend fun deleteEmails(emailDataSource: EmailsDataSource)

    fun getEmailCount(emailDataSource: EmailsDataSource): StateFlow<Int>

    fun returnAttachments(attachmentsDataSource: AttachmentsDataSource): MutableList<AttachmentsDAO>

    fun doAttachmentsExist(attachmentsDataSource: AttachmentsDataSource): Boolean

//    fun readEmail(email: EmailsDAO, emailsDataSource: EmailsDataSource, emailAddress: String, password: String): Boolean
}

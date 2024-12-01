package org.example.project

import com.composables.core.VerticalScrollbar
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import org.example.project.shared.data.AttachmentsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.EmailsDataSource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.ui.text.style.TextAlign
import com.composables.core.ScrollArea
import com.composables.core.Thumb
import com.composables.core.rememberScrollAreaState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import org.example.project.data.NewEmail
import org.example.project.networking.FirebaseAuthClient
import org.example.project.networking.OAuthResponse
import org.example.project.networking.TokenResponse
import org.example.project.shared.data.AccountsDAO
import org.example.project.shared.utils.createCompositeKey
import org.example.project.sqldelight.AccountsDataSource
import org.example.project.utils.NetworkError
import org.example.project.utils.onError
import org.example.project.utils.onSuccess


@OptIn(ExperimentalSettingsApi::class)
@Composable
@Preview
fun App(client: FirebaseAuthClient, emailService: EmailService, authentication: Authentication, driver: SqlDriver) {
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
        val accountsDataSource: AccountsDataSource = AccountsDataSource(database)

        // Basic Auth
        val emailCount by emailService.getEmailCount(emailDataSource).collectAsState()

        // Ui
        var isLoading by remember {
            mutableStateOf(false)
        }

        val scope = rememberCoroutineScope()

        var r by remember { mutableStateOf<OAuthResponse?>(null) }
        var e by remember {
            mutableStateOf<NetworkError?>(null)
        }

        authentication.amILoggedIn(accountsDataSource)
        val accountsExists = authentication.accountsExists(accountsDataSource)
        val amILoggedIn = authentication.isLoggedIn.collectAsState().value

        val accounts = remember { mutableStateOf(authentication.getAccounts(accountsDataSource)) }

        val allEmails = remember { mutableStateListOf<EmailsDAO>() }
        val allAttachments = remember { mutableStateListOf<AttachmentsDAO>() }

        var showEmails by remember { mutableStateOf(false) }


        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            modifier = Modifier.padding(16.dp).fillMaxSize()
        ) {
            Text(
                "Database Email Count: $emailCount"
            )
            r?.let {
                Text(it.email, color = Color.Green)
            }
            if (accountsExists) {
                LaunchedEffect(accounts.value) {

                    if (accounts.value.isEmpty()) {
                        // Clear UI state if there are no accounts
                        allEmails.clear()
                        allAttachments.clear()
                        showEmails = false
                        return@LaunchedEffect
                    }

                    allEmails.clear()
                    allAttachments.clear()
                    val emailsAndAttachments = getAllEmails(
                        emailDataSource,
                        emailQueries,
                        accountQueries,
                        emailService,
                        client,
                        accounts.value
                    )
                    allEmails.addAll(emailsAndAttachments.first.toSet())
                    allAttachments.addAll(emailsAndAttachments.second.toSet())
                    showEmails = true
                }
                Text("Logged in")
                println("Rendering emails. Current email count: ${allEmails.size}")


                LazyColumn {
                    items(accounts.value) { account ->
                        Text(account.email)
                        Button(onClick = {
                            scope.launch(Dispatchers.IO) {
                                showEmails = false
                                isLoading = true

                                authentication.logout(accountsDataSource, account.email)

                                // Update state
                                withContext(Dispatchers.Main) {
                                    // Update accounts first
                                    accounts.value = authentication.getAccounts(accountsDataSource)

                                    // Clear and potentially reload emails
                                    allEmails.clear()
                                    allAttachments.clear()

                                    if (accounts.value.isNotEmpty()) {
                                        val emailsAndAttachments = getAllEmails(
                                            emailDataSource,
                                            emailQueries,
                                            accountQueries,
                                            emailService,
                                            client,
                                            accounts.value
                                        )
                                        allEmails.addAll(emailsAndAttachments.first)
                                        allAttachments.addAll(emailsAndAttachments.second)
                                        showEmails = true
                                    }

                                    isLoading = false
                                }
                            }
                        }) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(15.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text("Logout")
                            }
                        }

                    }
                }
                Button(onClick = {
                    scope.launch {
                        showEmails = false
                        isLoading = true
                        val re = authentication.authenticateUser(client, accountsDataSource)
                        r = re.first
                        e = re.second

                        // Update state
                        accounts.value = authentication.getAccounts(accountsDataSource)

                        // Fetch emails for remaining accounts
                        val emailsAndAttachments = getAllEmails(
                            emailDataSource,
                            emailQueries,
                            accountQueries,
                            emailService,
                            client,
                            accounts.value
                        )
                        allEmails.clear()
                        allAttachments.clear()
                        allEmails.addAll(emailsAndAttachments.first)
                        allAttachments.addAll(emailsAndAttachments.second)

                        // Re-render the UI
                        showEmails = allEmails.isNotEmpty()
                        isLoading = false
                    }
                }) {
                    Text("Add another account (GMAIL)")
                }
                Button(onClick = {
                    scope.launch(Dispatchers.IO) {
                        emailService.deleteEmails(emailDataSource)
                    }
                }) {
                    Text("Manually Fetch emails")
                }
                Button(onClick = {
                    scope.launch(Dispatchers.IO) {
                        emailService.deleteEmails(emailDataSource)
                    }
                }) {
                    Text("Delete Emails")
                }
                if (showEmails) {
                    displayEmails(accounts.value, allEmails, allAttachments, emailDataSource, emailService)
                }
//                displayEmails(
//                    emailDataSource,
//                    emailQueries,
//                    accountQueries,
//                    emailService,
//                    amILoggedIn,
//                    authentication.email.value,
//                    client
//                )

            } else {
                Text(
                    text = "No Emails"
                )
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            isLoading = true
                            val re = authentication.authenticateUser(client, accountsDataSource)
                            r = re.first
                            e = re.second

                            // Update state
                            accounts.value = authentication.getAccounts(accountsDataSource)
                            isLoading = false
                        }
                    }
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }
}

suspend fun getAllEmails(
    emailDataSource: EmailsDataSource,
    emailTableQueries: EmailsTableQueries,
    accountQueries: AccountsTableQueries,
    emailService: EmailService,
    client: FirebaseAuthClient,
    accounts: List<AccountsDAO>
): Pair<MutableSet<EmailsDAO>, MutableSet<AttachmentsDAO>> {

    if (accounts.isEmpty()) {
        // Return empty lists if no accounts are present
        return Pair(mutableSetOf(), mutableSetOf())
    }


    // Fetch emails for all accounts

    val emails: MutableSet<EmailsDAO> = mutableSetOf()
    val attachments: MutableSet<AttachmentsDAO> = mutableSetOf()

    for ((index, account) in accounts.withIndex()) {
        print("Index  $index")
//        if (index == 0) {
        val r =
            emailService.getEmails(
                emailDataSource,
                emailTableQueries,
                accountQueries,
                account.email,
                client,
                accounts
            )
        emails.addAll(r.first)
        attachments.addAll(r.second)
//        }
    }

    return Pair(emails, attachments)
}

@Composable
fun displayEmails(
    accounts: List<AccountsDAO>,
    allEmails: MutableList<EmailsDAO>,
    allAttachments: MutableList<AttachmentsDAO>,
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

    val snapshotEmails by remember { derivedStateOf { allEmails.toList() } }

    // Create a derived state that updates when accounts change
    val filteredEmails = remember(accounts) {
        allEmails.filter { email ->
            accounts.any { it.email == email.account }
        }.toMutableList()
    }

    // Modify the list to remove emails from logged-out accounts
    LaunchedEffect(accounts) {
        // Create a set of valid account emails
        val validAccountEmails = accounts.map { it.email }.toSet()

        // Remove emails that don't belong to current accounts
        allEmails.removeAll { email ->
            email.account !in validAccountEmails
        }

        // Similarly, remove orphaned attachments
        allAttachments.removeAll { attachment ->
            // Remove attachments for emails that are no longer in the list
            allEmails.none { it.id == attachment.emailId }
        }
    }


    if (allEmails.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            // Email
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = {
                        sendEmail = true
                    }) {
                    Text(text = "Send Email")
                }
            }
//        ScrollArea(state = scrollState) {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {

                allEmails.forEach { email ->

                    val emailAddress = accounts.find { it.email == email.account }?.email ?: "Unknown Account"
                    var isRead by remember { mutableStateOf(email.isRead) }
                    println("Emails ${email.subject}")
                    Column(
                        modifier = Modifier.border(
                            width = 1.dp,
                            color = Color.DarkGray,
                            shape = RoundedCornerShape(4.dp)
                        ).background(
                            color = Color.LightGray
                        ).fillMaxWidth()
                    ) {
                        Text(text = "Account $emailAddress", color = Color.hsl(38f, 0.5f, 0.5f))
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
                        Text("Email read: $isRead")
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
                        Button(
                            onClick = {
                                CoroutineScope(Dispatchers.IO).launch {
                                    deleteEmail(email, emailDataSource, emailService, emailAddress)
                                    // Remove email on the main thread
                                    withContext(Dispatchers.Main) {
                                        allEmails.remove(email)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
                        ) {
                            Text(text = "Delete")
                        }
                        if (allAttachments.any { it.emailId === email.id }) {
                            Row {
                                allAttachments.filter { it.emailId === email.id }.forEach { attachment ->
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
        }
//        VerticalScrollbar(
//                modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().width(4.dp)
//            ) {
//                Thumb(Modifier.background(Color.Black))
//            }
    } else {
        // Optionally show a placeholder or message
        Text("No emails to display", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
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


//@Composable
//fun displayEmails(
//    emailDataSource: EmailsDataSource,
//    emailTableQueries: EmailsTableQueries,
//    accountQueries: AccountsTableQueries,
//    emailService: EmailService,
//    loggedIn: Boolean,
//    emailAddress: String,
//    client: FirebaseAuthClient
//) {
//    var display: Boolean by remember { mutableStateOf(false) }
//    var emailFromUser: String by remember { mutableStateOf("") }
//    var emailSubject: String by remember { mutableStateOf("") }
//    var emailContent: String by remember { mutableStateOf("") }
//
//    // Coroutine Scope
//    var currentProgress by remember { mutableStateOf(0f) }
//    var loading by remember { mutableStateOf(false) }
//    val scope = rememberCoroutineScope() // Create a coroutine scope
//
//    // Send email
//    var sendEmail by remember { mutableStateOf(false) }
//
//
//    fun displayEmailBody(show: Boolean, email: EmailsDAO) {
//
//        emailFromUser = ""
//        emailSubject = ""
//        emailContent = ""
//
//        if (show) {
//            display = true
//            emailFromUser = email.sender ?: ""
//            emailSubject = email.subject ?: ""
//            emailContent = email.body ?: ""
//        }
//    }
//
//    val state = rememberWebViewStateWithHTMLData(emailContent)
//    LaunchedEffect(Unit) {
//        state.webSettings.apply {
//            logSeverity = KLogSeverity.Debug
//            customUserAgentString =
//                "Mozilla/5.0 (Macintosh; Intel Mac OS X 11_1) AppleWebKit/625.20 (KHTML, like Gecko) Version/14.3.43 Safari/625.20"
//        }
//    }
//    val navigator = rememberWebViewNavigator()
//    var textFieldValue by remember(state.lastLoadedUrl) {
//        mutableStateOf(state.lastLoadedUrl)
//    }
//
//
//    val loadingState = state.loadingState
//    if (loadingState is LoadingState.Loading) {
//        LinearProgressIndicator(
//            progress = loadingState.progress,
//            modifier = Modifier.fillMaxWidth(),
//        )
//    }
//
//    var interger = intArrayOf(0).asFlow()
//
//    if (loggedIn && emailAddress.isNotEmpty()) {
//
//        // Email
//
//
//        var isLoading by remember { mutableStateOf(false) }
//        var emails by remember { mutableStateOf<List<EmailsDAO>?>(null) } // Store emails
//        var attachments by remember { mutableStateOf<List<AttachmentsDAO>>(emptyList()) } // Store attachments
//
//        val totalEmails by emailService.totalEmails.collectAsState()
//        val emailsReadCount by emailService.emailsRead.collectAsState()
//
//        // Vertical scrollbar
//        var lazyListState = rememberLazyListState()
//        val scrollState = rememberScrollAreaState(lazyListState)
//
//
//        LaunchedEffect(emailsReadCount) {
//            loadProgress(emailsReadCount, totalEmails) { progress ->
//                currentProgress = progress
//            }
//        }
//
//        LaunchedEffect(Unit) { // Trigger once
//            isLoading = true
//            try {
//                // Replace with your actual email retrieval logic
//
//                val startTime = Clock.System.now()
//                val returned = withContext(Dispatchers.IO) {
//                    emailService.getEmails(
//                        emailDataSource,
//                        emailTableQueries,
//                        accountQueries,
//                        emailAddress,
//                        client
//                    )
//                }
//                val endTime = Clock.System.now()
//                val duration = endTime - startTime
//                println("Emails loaded in ${duration.inWholeSeconds} seconds or ${duration.inWholeMilliseconds} ms")
//
//
//                withContext(Dispatchers.Main) {
//                    attachments = returned.second
//                    isLoading = false // Hide loading indicator after updating emails
//                }
//
//            } catch (e: Exception) {
//                // Handle error, e.g., show an error message
//                println("Error in App ${e.message}")
//                withContext(Dispatchers.Main) {
//                    isLoading = false
//                }
//            } finally {
//                isLoading = false
//            }
//        }
//
//        if (isLoading) {
//            Column(
//                modifier = Modifier.fillMaxSize(),
//                verticalArrangement = Arrangement.Center,
//                horizontalAlignment = Alignment.CenterHorizontally
//            ) {
//                Text("Loading emails...")
//                Text("Emails read: ${emailsReadCount} out of ${totalEmails}")
//                LinearProgressIndicator(
////                    progress = currentProgress,
//                    modifier = Modifier.fillMaxWidth()
//                )
//            }
//        } else {
//            // Display email content
//
//            emails = emailDataSource.selectAllEmailsFlow().collectAsState(initial = emptyList()).value
//
//            println("Emails: ${emails!!.size}")
//
//            if (emails != null) {
//                Column(modifier = Modifier.fillMaxHeight(0.7f), verticalArrangement = Arrangement.SpaceBetween) {
//                    // Email
//                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
//                        Button(
//                            onClick = {
//                                sendEmail = true
//                            }) {
//                            Text(text = "Send Email")
//                        }
//                    }
//                    ScrollArea(state = scrollState) {
//
//                        LazyColumn(modifier = Modifier.fillMaxWidth(), state = lazyListState) {
//
//                            items(emails!!.sortedBy { it.receivedDate }.reversed()) { email ->
//
//                                var isRead by remember { mutableStateOf(email.isRead) }
//                                println("Email id...: ${email.subject} ${email.isRead}")
//                                Column(
//                                    modifier = Modifier.border(
//                                        width = 1.dp,
//                                        color = Color.DarkGray,
//                                        shape = RoundedCornerShape(4.dp)
//                                    ).background(
//                                        color = Color.LightGray
//                                    ).fillMaxWidth()
//                                ) {
//                                    Text(text = "Account $emailAddress")
//                                    Text(
//                                        text = email.sender ?: "No from",
//                                    )
//                                    Text(
//                                        text = email.subject ?: "No subject"
//                                    )
//                                    Button(
//                                        onClick = {
//                                            displayEmailBody(!display, email)
//                                        },
//                                    ) {
//                                        Text("View Email")
//                                    }
//                                    Text("Email read: ${isRead}")
//                                    Button(
//                                        onClick = {
//                                            CoroutineScope(Dispatchers.IO).launch {
//                                                isRead =
//                                                    read(email, emailDataSource, emailService, emailAddress)
//                                                        ?: false
//                                            }
//                                        }
//                                    ) {
//                                        Text(text = if (isRead) "Mark as unread" else "Mark as read")
//                                    }
//                                    Button(
//                                        onClick = {
//                                            CoroutineScope(Dispatchers.IO).launch {
//                                                deleteEmail(
//                                                    email,
//                                                    emailDataSource,
//                                                    emailService,
//                                                    emailAddress
//                                                )
//                                            }
//                                        },
//                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
//                                    ) {
//                                        Text(text = "Delete")
//                                    }
//                                    if (attachments.any { it.emailId === email.id }) {
//                                        Row {
//                                            attachments.filter { it.emailId === email.id }.forEach { attachment ->
//                                                Row {
//                                                    Text(
//                                                        text = attachment.fileName,
//                                                        modifier = Modifier
//                                                            .padding(8.dp)
//                                                    )
//                                                    Text(
//                                                        text = attachment.size.toString(), modifier = Modifier
//                                                            .padding(8.dp)
//
//                                                    )
//                                                    Text(
//                                                        text = attachment.mimeType, modifier = Modifier
//                                                            .padding(8.dp)
//                                                    )
//                                                }
//
//                                            }
//                                        }
//                                    } else {
//                                        Text(
//                                            text = "No attachments",
//                                        )
//                                    }
//                                }
//
//                            }
//
//                        }
//                        VerticalScrollbar(
//                            modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().width(4.dp)
//                        ) {
//                            Thumb(Modifier.background(Color.Black))
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    if (display) {
//        Dialog(
//            onDismissRequest = { display = false },
//            properties = DialogProperties(
//                dismissOnBackPress = true,
//                dismissOnClickOutside = true,
//                usePlatformDefaultWidth = false
//            ),
//        ) {
//            // Draw a rectangle shape with rounded corners inside the dialog
//            Surface(modifier = Modifier.fillMaxSize(0.6f)) {
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(525.dp)
//                        .padding(16.dp),
//                    shape = RoundedCornerShape(16.dp),
//                ) {
//                    Column {
//                        TopAppBar(
//                            title = { Text(text = "WebView Sample") },
//                            navigationIcon = {
//                                if (navigator.canGoBack) {
//                                    IconButton(onClick = { navigator.navigateBack() }) {
//                                        Icon(
//                                            imageVector = Icons.Default.ArrowBack,
//                                            contentDescription = "Back",
//                                        )
//                                    }
//                                }
//                            },
//                        )
//
//                        Row {
//                            Box(modifier = Modifier.weight(1f)) {
//                                if (state.errorsForCurrentRequest.isNotEmpty()) {
//                                    Image(
//                                        imageVector = Icons.Default.Close,
//                                        contentDescription = "Error",
//                                        colorFilter = ColorFilter.tint(Color.Red),
//                                        modifier =
//                                            Modifier
//                                                .align(Alignment.CenterEnd)
//                                                .padding(8.dp),
//                                    )
//                                }
//
//                                OutlinedTextField(
//                                    value = textFieldValue ?: "",
//                                    onValueChange = { textFieldValue = it },
//                                    modifier = Modifier.fillMaxWidth(),
//                                )
//                            }
//
//                            Button(
//                                onClick = {
//                                    textFieldValue?.let {
//                                        navigator.loadUrl(it)
//                                    }
//                                },
//                                modifier = Modifier.align(Alignment.CenterVertically),
//                            ) {
//                                Text("Go")
//                            }
//                        }
//                    }
//
//                    WebView(
//                        state = state,
//                        modifier =
//                            Modifier
//                                .fillMaxSize(),
//                        navigator = navigator,
//                    )
//
//                    Column(
//                        modifier = Modifier
//                            .fillMaxSize(),
//                        verticalArrangement = Arrangement.Center,
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                    ) {
//
//                        Text(
//                            text = "This is a dialog with buttons and an image.",
//                            modifier = Modifier.padding(16.dp),
//                        )
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth(),
//                            horizontalArrangement = Arrangement.Center,
//                        ) {
//                            TextButton(
//                                onClick = { /* TODO */ },
//                                modifier = Modifier.padding(8.dp),
//                            ) {
//                                Text("Dismiss")
//                            }
//                            TextButton(
//                                onClick = { /* TODO*/ },
//                                modifier = Modifier.padding(8.dp),
//                            ) {
//                                Text("Confirm")
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    if (sendEmail) {
//        var sendEmailFrom by remember { mutableStateOf("") }
//        var sendEmailTo by remember { mutableStateOf("") }
//        var sendEmailSubject by remember { mutableStateOf("") }
//        var sendEmailBody by remember { mutableStateOf("") }
//
//        fun sendEmail() {
//            println("Sending email... $sendEmailFrom, $sendEmailTo, $sendEmailSubject, $sendEmailBody")
//            var sentEmailSuccess = false
//            CoroutineScope(Dispatchers.IO).launch {
//                sentEmailSuccess = emailService.sendNewEmail(
//                    emailDataSource,
//                    NewEmail(from = sendEmailFrom, to = sendEmailTo, subject = sendEmailSubject, body = sendEmailBody),
//                    emailAddress
//                )
//            }
//
//            println("Email sent successfully? $sentEmailSuccess")
//
//            sendEmail = false
//        }
//
//        Dialog(
//            onDismissRequest = { sendEmail = false },
//            properties = DialogProperties(
//                dismissOnBackPress = true,
//                dismissOnClickOutside = true,
//                usePlatformDefaultWidth = false
//            ),
//        ) {
//            // Draw a rectangle shape with rounded corners inside the dialog
//            Surface(modifier = Modifier.fillMaxSize(0.9f)) {
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(525.dp)
//                        .padding(16.dp),
//                    shape = RoundedCornerShape(16.dp),
//                ) {
//                    Column(
//                        modifier = Modifier
//                            .fillMaxSize(),
//                        verticalArrangement = Arrangement.Center,
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                    ) {
//                        TextField(
//                            label = { Text("From") },
//                            value = sendEmailFrom,
//                            onValueChange = { sendEmailFrom = it },
//                            modifier = Modifier.padding(16.dp),
//                        )
//                        TextField(
//                            label = { Text("To") },
//                            value = sendEmailTo,
//                            onValueChange = { sendEmailTo = it },
//                            modifier = Modifier.padding(16.dp)
//                        )
//                        TextField(
//                            label = { Text("Subject") },
//                            value = sendEmailSubject,
//                            onValueChange = { sendEmailSubject = it },
//                            modifier = Modifier.padding(16.dp)
//                        )
//                        TextField(
//                            label = { Text("Body") },
//                            value = sendEmailBody,
//                            onValueChange = { sendEmailBody = it },
//                            modifier = Modifier.padding(16.dp),
//                        )
//                        Button(onClick = {
//                            sendEmail()
//                        }) {
//                            Text(text = "Send")
//                        }
//                    }
//                }
//            }
//        }
//    }
//}

fun read(
    email: EmailsDAO,
    emailsDataSource: EmailsDataSource,
    emailService: EmailService,
    emailAddress: String,
): Boolean? {

    val emailCompKey = createCompositeKey(email.subject, email.receivedDate, email.sender)

    val emailRead = emailService.readEmail(email, emailsDataSource, emailAddress)

    if (emailRead.first) {
        emailsDataSource.updateEmailReadStatus(
            emailCompKey,
            !email.isRead
        )
    }

    return emailRead.second

}

fun deleteEmail(
    email: EmailsDAO,
    emailsDataSource: EmailsDataSource,
    emailService: EmailService,
    emailAddress: String,
) {

    val emailCompKey = createCompositeKey(email.subject, email.receivedDate, email.sender)
    val emailDeleted = emailService.deleteEmail(email, emailsDataSource, emailAddress)

//    println("Deleting suc")
    if (emailDeleted) {

        println("Delete successful $emailDeleted")

        emailsDataSource.deleteEmail(
            email.id!!
        )
    }
}

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
        client: FirebaseAuthClient,
        accounts: List<AccountsDAO>
    ): Pair<MutableList<EmailsDAO>, MutableList<AttachmentsDAO>>

    suspend fun deleteEmails(emailDataSource: EmailsDataSource)

    fun getEmailCount(emailDataSource: EmailsDataSource): StateFlow<Int>

    fun returnAttachments(attachmentsDataSource: AttachmentsDataSource): MutableList<AttachmentsDAO>

    fun doAttachmentsExist(attachmentsDataSource: AttachmentsDataSource): Boolean

    fun readEmail(
        email: EmailsDAO,
        emailsDataSource: EmailsDataSource,
        emailAddress: String
    ): Pair<Boolean, Boolean?>

    fun deleteEmail(
        email: EmailsDAO,
        emailsDataSource: EmailsDataSource,
        emailAddress: String
    ): Boolean

    fun sendNewEmail(
        emailsDataSource: EmailsDataSource,
        newEmail: NewEmail,
        emailAddress: String
    ): Boolean


}

expect suspend fun openBrowser(): String

expect class Authentication {

    val isLoggedIn: StateFlow<Boolean>
    val email: StateFlow<String>

    suspend fun authenticateUser(
        fAuthClient: FirebaseAuthClient,
        accountsDataSource: AccountsDataSource
    ): Pair<OAuthResponse?, NetworkError?>

    fun amILoggedIn(accountsDataSource: AccountsDataSource): Boolean
    fun accountsExists(accountsDataSource: AccountsDataSource): Boolean
    fun getAccounts(accountsDataSource: AccountsDataSource): List<AccountsDAO>
    fun checkIfTokenExpired(accountsDataSource: AccountsDataSource): Boolean
    fun logout(accountsDataSource: AccountsDataSource, email: String)
}
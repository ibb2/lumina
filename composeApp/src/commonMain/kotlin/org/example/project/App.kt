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
import com.composables.core.ScrollArea
import com.composables.core.Thumb
import com.composables.core.rememberScrollAreaState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import org.example.project.data.NewEmail
import org.example.project.networking.FirebaseAuthClient
import org.example.project.networking.OAuthResponse
import org.example.project.networking.TokenResponse
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

        var login by remember {
            mutableStateOf(false)
        }

        var isLoading by remember {
            mutableStateOf(false)
        }

        var errorMsg by remember {
            mutableStateOf<NetworkError?>(null)
        }

        var oAuthResponse by remember {
            mutableStateOf<OAuthResponse?>(null)
        }

        val scope = rememberCoroutineScope()
        var authenticationCode by remember { mutableStateOf("") }
        var tResponse by remember { mutableStateOf<TokenResponse?>(null) }
        var tError by remember {
            mutableStateOf<NetworkError?>(null)
        }

        var r by remember { mutableStateOf<OAuthResponse?>(null) }
        var e by remember {
            mutableStateOf<NetworkError?>(null)
        }

        authentication.amILoggedIn(accountsDataSource)
        val amILoggedIn = authentication.isLoggedIn.collectAsState().value

        if (amILoggedIn) {
            Text("Logged in")
        } else {
            Text("Not logged in")
        }

        if (!loggedIn) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                Text(
                    "Database Email Count: $emailCount"
                )
                r?.let {
                    Text(it.email, color = Color.Green)
                }
                e?.let {
                    Text(it.name, color = Color.Magenta)
                }
                errorMsg?.let {
                    Text(it.name, color = Color.Red)
                }
                tError?.let {
                    Text(it.name, color = Color.Yellow)
                }
                Button(
                    onClick = {
                        scope.launch {
                            val re = authentication.authenticateUser(client, accountsDataSource)
                            r = re.first
                            e = re.second
                        }
                    }
                ) {
                    Text("Google Sign in full")
                }
                Button(onClick = {
                    runBlocking {
                        authenticationCode = openBrowser()
                        client.googleTokenIdEndpoint(authenticationCode).onSuccess {
                            tResponse = it
                        }.onError {
                            tError = it
                        }
                    }
                }) {
                    Text(text = "Open Browser to sign in")
                }
                Button(onClick = {
                    scope.launch {
                        println("Auth Code: $authenticationCode")
                        client.googleLogin(tResponse!!.idToken ?: "").onSuccess {
                            oAuthResponse = it
                            isLoading = false
                        }.onError {
                            isLoading = false
                            errorMsg = it
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
                        Text("Sign in with Google")
                    }
                }
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
//                    login(observableSettings, accountQueries, emailAddress, password)
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
            Button(onClick = {
                scope.launch(Dispatchers.IO) {
                    emailService.deleteEmails(emailDataSource)
                }
            }) {
                Text("Delete Emails")
            }

//            displayEmails(
//                emailDataSource,
//                observableSettings,
//                emailQueries,
//                accountQueries,
//                emailService,
//                loggedIn,
//                emailAddress,
//                password
//            )
        }
    }
}

fun authentication() {


}

//fun login(
//    observableSettings: ObservableSettings,
//    accountQueries: AccountsTableQueries,
//    emailAddress: String,
//    password: String
//): Unit {
//
//    val em = observableSettings.getString(
//        "emailAddress",
//        defaultValue = ""
//    )
//    val pw = observableSettings.getString("password", defaultValue = "")
//
//    try {
//        observableSettings.putString("emailAddress", em)
//        observableSettings.putString("password", pw)
//        observableSettings.putBoolean("login", true)
//    } catch (e: NullPointerException) {
//
//        observableSettings.putString("emailAddress", emailAddress)
//        observableSettings.putString("password", password)
//        observableSettings.putBoolean("login", true)
//    }
//    try {
//        accountQueries.selectAccount(emailAddress).executeAsOneOrNull()
//            ?: throw NullPointerException()
//    } catch (e: NullPointerException) {
//        accountQueries.insertAccount(emailAddress)
//    }
//
//    println("Logged in as $emailAddress")
//}

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

    // Send email
    var sendEmail by remember { mutableStateOf(false) }


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

    var interger = intArrayOf(0).asFlow()

    if (loggedIn && emailAddress.isNotEmpty() && password.isNotEmpty()) {

        // Email


        var isLoading by remember { mutableStateOf(false) }
        var emails by remember { mutableStateOf<List<EmailsDAO>?>(null) } // Store emails
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

            emails = emailDataSource.selectAllEmailsFlow().collectAsState(initial = emptyList()).value

            println("Emails: ${emails!!.size}")

            if (emails != null) {
                Column(modifier = Modifier.fillMaxHeight(0.7f), verticalArrangement = Arrangement.SpaceBetween) {
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

                        LazyColumn(modifier = Modifier.fillMaxWidth(), state = lazyListState) {

                            items(emails!!.sortedBy { it.receivedDate }.reversed()) { email ->

                                var isRead by remember { mutableStateOf(email.isRead) }
                                println("Email id...: ${email.subject} ${email.isRead}")
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
                                    Text("Email read: ${isRead}")
                                    Button(
                                        onClick = {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                isRead =
                                                    read(email, emailDataSource, emailService, emailAddress, password)
                                                        ?: false
                                            }
                                        }
                                    ) {
                                        Text(text = if (isRead) "Mark as unread" else "Mark as read")
                                    }
                                    Button(
                                        onClick = {
                                            CoroutineScope(Dispatchers.IO).launch {
                                                deleteEmail(
                                                    email,
                                                    emailDataSource,
                                                    emailService,
                                                    emailAddress,
                                                    password
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red),
                                    ) {
                                        Text(text = "Delete")
                                    }
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
                    NewEmail(from = sendEmailFrom, to = sendEmailTo, subject = sendEmailSubject, body = sendEmailBody),
                    emailAddress,
                    password
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

fun read(
    email: EmailsDAO,
    emailsDataSource: EmailsDataSource,
    emailService: EmailService,
    emailAddress: String,
    password: String,
): Boolean? {

    val emailCompKey = createCompositeKey(email.subject, email.receivedDate, email.sender)

    val emailRead = emailService.readEmail(email, emailsDataSource, emailAddress, password)

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
    password: String,
) {

    val emailCompKey = createCompositeKey(email.subject, email.receivedDate, email.sender)
    val emailDeleted = emailService.deleteEmail(email, emailsDataSource, emailAddress, password)

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
        password: String
    ): Pair<MutableList<EmailsDAO>, MutableList<AttachmentsDAO>>

    suspend fun deleteEmails(emailDataSource: EmailsDataSource)

    fun getEmailCount(emailDataSource: EmailsDataSource): StateFlow<Int>

    fun returnAttachments(attachmentsDataSource: AttachmentsDataSource): MutableList<AttachmentsDAO>

    fun doAttachmentsExist(attachmentsDataSource: AttachmentsDataSource): Boolean

    fun readEmail(
        email: EmailsDAO,
        emailsDataSource: EmailsDataSource,
        emailAddress: String,
        password: String
    ): Pair<Boolean, Boolean?>

    fun deleteEmail(
        email: EmailsDAO,
        emailsDataSource: EmailsDataSource,
        emailAddress: String,
        password: String
    ): Boolean

    fun sendNewEmail(
        emailsDataSource: EmailsDataSource,
        newEmail: NewEmail,
        emailAddress: String,
        password: String
    ): Boolean


}

expect suspend fun openBrowser(): String

expect class Authentication {

    val isLoggedIn: StateFlow<Boolean>

    suspend fun authenticateUser(fAuthClient: FirebaseAuthClient, accountsDataSource: AccountsDataSource): Pair<OAuthResponse?, NetworkError?>

    fun amILoggedIn(accountsDataSource: AccountsDataSource): Boolean
    fun checkIfTokenExpired(accountsDataSource: AccountsDataSource): Boolean
}
package org.example.project

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.example.Emails
import com.example.project.database.LuminaDatabase
import com.russhwolf.settings.ExperimentalSettingsApi
import org.example.project.shared.data.AttachmentsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.EmailsDataSource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material.*
import androidx.compose.ui.text.input.TextFieldValue
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.konyaco.fluent.component.SideNav
import com.konyaco.fluent.component.SideNavHeader
import com.konyaco.fluent.component.SideNavItem
import com.konyaco.fluent.icons.Icons
import com.konyaco.fluent.icons.regular.Circle
import com.konyaco.fluent.icons.regular.Settings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.example.project.data.NewEmail
import org.example.project.networking.FirebaseAuthClient
import org.example.project.networking.OAuthResponse
import org.example.project.screen.HomeScreen
import org.example.project.screen.SettingsScreen
import org.example.project.shared.data.AccountsDAO
import org.example.project.shared.data.FoldersDAO
import org.example.project.shared.utils.createCompositeKey
import org.example.project.sqldelight.AccountsDataSource
import org.example.project.ui.displayEmails
import org.example.project.ui.platformSpecific.PlatformSpecificText
import org.example.project.ui.platformSpecific.PlatformSpecificTextField
import org.example.project.ui.platformSpecific.buttons.PlatformSpecificIconButton
import org.example.project.utils.NetworkError

@OptIn(ExperimentalSettingsApi::class)
@Composable
@Preview
fun App(client: FirebaseAuthClient, emailService: EmailService, authentication: Authentication, driver: SqlDriver) {


    val currentSystemTheme = isSystemInDarkTheme()

    Navigator(
        HomeScreen(
            homepageContent = {
                PlatformSpecificUI(
                    modifier = Modifier,
                    currentSystemTheme = currentSystemTheme,
                ) { Main(client, emailService, authentication, driver, it) }
            }
        )) {
        SlideTransition(it)
    }


}

@Composable
expect fun PlatformSpecificUI(modifier: Modifier, currentSystemTheme: Boolean, content: @Composable () -> Unit): Unit


@Composable
fun Main(
    client: FirebaseAuthClient,
    emailService: EmailService,
    authentication: Authentication,
    driver: SqlDriver,
    localNavigator: Navigator
) {

    // db related stuff
    val database = LuminaDatabase(
        driver,
        EmailsAdapter = Emails.Adapter(
            attachments_countAdapter = IntColumnAdapter
        )
    )

    // Theme
    val isSystemInDarkMode = isSystemInDarkTheme()

    // Data Sources
    val emailDataSource: EmailsDataSource = EmailsDataSource(database)
    val attachmentsDataSource: AttachmentsDataSource = AttachmentsDataSource(database)
    val accountsDataSource: AccountsDataSource = AccountsDataSource(database)

    val scope = rememberCoroutineScope()

    var r by remember { mutableStateOf<OAuthResponse?>(null) }
    var e by remember {
        mutableStateOf<NetworkError?>(null)
    }

    authentication.amILoggedIn(accountsDataSource)

    val accounts = remember { mutableStateOf(authentication.getAccounts(accountsDataSource)) }
    val emailServiceManager = remember {
        EmailServiceManager(
            emailService,
            emailDataSource
        )
    }

    LaunchedEffect(accounts.value) {
        withContext(Dispatchers.Default) {
            emailServiceManager.syncEmails(
                accounts.value
            )
            emailServiceManager.getFolders(
                accounts.value
            )
            emailServiceManager.watchEmails(
                accounts.value,
                emailDataSource
            )
        }
    }

    val folders by emailServiceManager.folders.collectAsState()
    val emails by emailServiceManager.emails.collectAsState()
    val attachments by emailServiceManager.attachments.collectAsState()
    val isSyncing by emailServiceManager.isSyncing.collectAsState()
    val isSearching by emailServiceManager.isSearching.collectAsState()
    val allEmails = remember { mutableStateOf<List<EmailsDAO>>(emptyList()) }

    // Change how you handle emailsFlow
    LaunchedEffect(Unit) {
        println("Setting up email flow collection")
        emailDataSource.emailsFlow
            .collect { emails ->
                println("CRITICAL DEBUG: Collected ${emails.size} emails")
                // Use withContext to ensure UI update happens on Main dispatcher
                withContext(Dispatchers.Main) {
                    allEmails.value = emails
                }
            }
    }

    val selectedFolders = remember { mutableStateOf<List<String>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        emailServiceManager.search(searchQuery, isDeleting)
    }

    // UI with sync indicator

    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }

    SideNav(
        modifier = Modifier.fillMaxHeight(),
        expanded = expanded,
        onExpandStateChange = { expanded = it },
        title = { PlatformSpecificText("Sidebar") }) {

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.fillMaxHeight()) {
                repeat(6) { index ->
                    SideNavItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        content = { com.konyaco.fluent.component.Text("Menu Item $index") },
                        icon = {
                            com.konyaco.fluent.component.Icon(
                                imageVector = Icons.Default.Circle,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
            SideNavItem(
                selected = selectedIndex == 7,
                onClick = {
                    localNavigator.push(
                        SettingsScreen(
                            client,
                            driver,
                            emailService,
                            authentication,
                            accountsDataSource,
                            emailDataSource,
                            attachmentsDataSource
                        )
                    )
                },
                content = { PlatformSpecificText("Settings", fontSize = 16) },
                icon = {
                    com.konyaco.fluent.component.Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            )
        }
    }
//    Column(
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.SpaceBetween,
//        modifier = Modifier.padding(16.dp).fillMaxSize()
//    ) {
//
//        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
//
////                TextField(
////                    modifier = Modifier.border(BorderStroke(0.dp, Color.Transparent), CircleShape),
////                    value = searchQuery,
////                    onValueChange = {
////                        isDeleting = searchQuery.length > it.length
////                        searchQuery = it
////                    },
////                    label = { Text("Search") },
//////                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
////                )
//
//                var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
//
//                fun updateTextFieldValue(newValue: TextFieldValue) {
//                    textFieldValue = newValue
//                }
//
//                PlatformSpecificTextField(Modifier, textFieldValue) {
//                    updateTextFieldValue(it)
//                }
//        }
//
////            if (folders.size > 0) {
////                LazyRow(modifier = Modifier.fillMaxHeight(0.3f)) {
////                    itemsIndexed(folders) { _, it ->
////                        Row {
////                            val isSelected = selectedFolders.value.contains(it.name)
////                            val color = if (isSelected) Color.Green else Color.White
////                            Button(
////                                onClick = {
////                                    println("Folder selected ${it.name}")
////                                    val currentFolders = selectedFolders.value.toMutableList()
////                                    if (currentFolders.contains(it.name)) {
////                                        currentFolders.remove(it.name)
////                                    } else {
////                                        currentFolders.add(it.name)
////                                    }
////                                    // Update the entire list
////                                    selectedFolders.value = currentFolders
////                                },
////                                colors = ButtonDefaults.buttonColors(color)
////                            ) {
////                                Text(it.name)
////                            }
////                        }
////                        Divider(modifier = Modifier.width(4.dp))
////                    }
////                }
////            }
//
//        if (isSyncing) {
//            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
//        }
//
//        // Use emails and attachments in your display logic
//        if (isSearching && emails.isEmpty()) {
//            Text("No emails found :)")
//        }
//
//        displayEmails(
//            accounts = accounts.value,
//            selectedFolders = selectedFolders,
//            emails = allEmails.value.toMutableList(),
//            attachments = attachments,
//            emailDataSource = emailDataSource,
//            client = client,
//            emailService = emailService,
//            authentication = authentication,
//            driver = driver,
//            localNavigator = localNavigator,
//            accountsDataSource = accountsDataSource,
//            attachmentsDataSource = attachmentsDataSource,
//        )
//    }
}

class EmailServiceManager(
    private val emailService: EmailService,
    private val emailsDataSource: EmailsDataSource
) {
    private val _folders = MutableStateFlow<MutableList<FoldersDAO>>(mutableListOf())
    val folders: StateFlow<MutableList<FoldersDAO>> = _folders.asStateFlow()

    private val _emails = MutableStateFlow<MutableList<EmailsDAO>>(mutableListOf())
    val emails: StateFlow<MutableList<EmailsDAO>> = _emails.asStateFlow()

    private val _attachments = MutableStateFlow<MutableList<AttachmentsDAO>>(mutableListOf())
    val attachments: StateFlow<MutableList<AttachmentsDAO>> = _attachments.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()


    private val _search = MutableStateFlow<MutableList<EmailsDAO>>(mutableListOf())
    val search: StateFlow<MutableList<EmailsDAO>> = _search.asStateFlow()

    suspend fun syncEmails(accounts: MutableList<AccountsDAO>) {
        if (accounts.isEmpty()) {
            _emails.value = mutableListOf()
            _attachments.value = mutableListOf()
            return
        }

        _isSyncing.value = true
        try {
            // Perform email sync in parallel
            val emailResults = coroutineScope {
                accounts.map {
                    async {
                        emailService.getEmails(
                            it.email,
                        )
                    }
                }.awaitAll()
            }

            emailResults
//            _emails.value = mutableListOf()
//            _attachments.value = mutableListOf()

            // Combine results from all accounts
            val combinedEmails = emailResults.flatMap { it.first.value }
            val combinedAttachments = emailResults.flatMap { it.second.value }

            // Update state flows
            _emails.value.addAll(combinedEmails)
            _attachments.value.addAll(combinedAttachments)
        } catch (e: Exception) {
            // Log error or handle synchronization failure
            println("Email sync failed: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun getFolders(accounts: MutableList<AccountsDAO>) {
        if (accounts.isEmpty()) {
            _folders.value = mutableListOf()
            return
        }

        _isSyncing.value = true
        try {
            // Perform email sync in parallel
            val foldersResults = coroutineScope {
                accounts.map {
                    async {
                        emailService.getFolders(
                            it.email,
                        )
                    }
                }.awaitAll()
            }

            // Combine results from all accounts
            val combinedFolders = foldersResults.flatten()
            println("Folders $combinedFolders")

            // Update state flows
            _folders.value.addAll(combinedFolders)
        } catch (e: Exception) {
            // Log error or handle synchronization failure
            println("Fetching folders failed: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    suspend fun search(search: String, deleting: Boolean) {
        println("Searching... $search $deleting")
        if (search.isEmpty()) {
            _emails.value = mutableListOf()
            _emails.value.addAll(_search.value)
            _isSearching.value = false
            return
        }

        if (search.length == 1 && !deleting) {
            _search.value.addAll(emails.value)
        }

        _isSearching.value = true

        try {

            // Perform email sync in parallel
            val searchResults = coroutineScope {
                async {
                    emailService.searchEmails(
                        query = search,
                    )
                }.await()
            }

            println("Results ${searchResults}")

            _emails.value.clear()
            _emails.value.addAll(searchResults)
        } catch (e: Exception) {
            // Log error or handle synchronization failure
            println("Search failed: ${e.message}")
        } finally {
            _isSyncing.value = false
        }

    }

    suspend fun watchEmails(accounts: MutableList<AccountsDAO>, emailsDataSource: EmailsDataSource) {
        accounts.map {
            emailService.watchEmails(it.email, emailsDataSource)
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

    val emailCompKey = createCompositeKey(email.subject, email.receivedDate, email.senderAddress)

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

    val emailCompKey = createCompositeKey(email.subject, email.receivedDate, email.senderAddress)
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


expect class EmailService(
    client: FirebaseAuthClient,
) {

    val folders: StateFlow<MutableList<FoldersDAO>>
    val emails: StateFlow<MutableList<EmailsDAO>>
    val attachments: StateFlow<MutableList<AttachmentsDAO>>
    val isSyncing: StateFlow<Boolean>

    suspend fun getEmails(emailAddress: String): Pair<StateFlow<List<EmailsDAO>>, StateFlow<List<AttachmentsDAO>>>

    suspend fun watchEmails(emailAddress: String, dataSource: EmailsDataSource)

    suspend fun searchEmails(query: String): List<EmailsDAO>

    suspend fun getFolders(emailAddress: String): MutableList<FoldersDAO>

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

expect class Authentication {

    val isLoggedIn: StateFlow<Boolean>
    val email: StateFlow<String>

    suspend fun authenticateUser(
        fAuthClient: FirebaseAuthClient,
        accountsDataSource: AccountsDataSource
    ): Pair<OAuthResponse?, NetworkError?>

    fun amILoggedIn(accountsDataSource: AccountsDataSource): Boolean
    fun accountsExists(accountsDataSource: AccountsDataSource): Boolean
    fun getAccounts(accountsDataSource: AccountsDataSource): MutableList<AccountsDAO>
    fun checkIfTokenExpired(accountsDataSource: AccountsDataSource): Boolean
    fun logout(accountsDataSource: AccountsDataSource, email: String)
}
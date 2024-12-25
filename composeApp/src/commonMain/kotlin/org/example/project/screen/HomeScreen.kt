package org.example.project.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.Emails
import com.example.project.database.LuminaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.Authentication
import org.example.project.EmailService
import org.example.project.EmailServiceManager
import org.example.project.networking.FirebaseAuthClient
import org.example.project.networking.OAuthResponse
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.AccountsDataSource
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.EmailsDataSource
import org.example.project.ui.displayEmails
import org.example.project.ui.platformSpecific.PlatformSpecificTextField
import org.example.project.utils.NetworkError

data class HomeScreen(
    val client: FirebaseAuthClient,
    val emailService: EmailService,
    val authentication: Authentication,
    val driver: SqlDriver,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Main(
            client = client,
            emailService = emailService,
            authentication = authentication,
            driver = driver,
            localNavigator = navigator
        )
    }

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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp).fillMaxHeight()
        ) {

            Box( contentAlignment = Alignment.Center) {

//                TextField(
//                    modifier = Modifier.border(BorderStroke(0.dp, Color.Transparent), CircleShape),
//                    value = searchQuery,
//                    onValueChange = {
//                        isDeleting = searchQuery.length > it.length
//                        searchQuery = it
//                    },
//                    label = { Text("Search") },
////                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
//                )

                var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

                fun updateTextFieldValue(newValue: TextFieldValue) {
                    textFieldValue = newValue
                }

                PlatformSpecificTextField(Modifier, textFieldValue) {
                    updateTextFieldValue(it)
                }
            }

//            if (folders.size > 0) {
//                LazyRow(modifier = Modifier.fillMaxHeight(0.3f)) {
//                    itemsIndexed(folders) { _, it ->
//                        Row {
//                            val isSelected = selectedFolders.value.contains(it.name)
//                            val color = if (isSelected) Color.Green else Color.White
//                            Button(
//                                onClick = {
//                                    println("Folder selected ${it.name}")
//                                    val currentFolders = selectedFolders.value.toMutableList()
//                                    if (currentFolders.contains(it.name)) {
//                                        currentFolders.remove(it.name)
//                                    } else {
//                                        currentFolders.add(it.name)
//                                    }
//                                    // Update the entire list
//                                    selectedFolders.value = currentFolders
//                                },
//                                colors = ButtonDefaults.buttonColors(color)
//                            ) {
//                                Text(it.name)
//                            }
//                        }
//                        Divider(modifier = Modifier.width(4.dp))
//                    }
//                }
//            }

            if (isSyncing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Use emails and attachments in your display logic
            if (isSearching && emails.isEmpty()) {
                Text("No emails found :)")
            }

            displayEmails(
                accounts = accounts.value,
                selectedFolders = selectedFolders,
                emails = allEmails.value.toMutableList(),
                attachments = attachments,
                emailDataSource = emailDataSource,
                client = client,
                emailService = emailService,
                authentication = authentication,
                driver = driver,
                localNavigator = localNavigator,
                accountsDataSource = accountsDataSource,
                attachmentsDataSource = attachmentsDataSource,
            )
        }
    }

}


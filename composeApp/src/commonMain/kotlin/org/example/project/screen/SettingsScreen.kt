package org.example.project.screen

import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.Emails
import com.example.project.database.LuminaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
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
import org.example.project.ui.FluentThemeEntry
import org.example.project.ui.platformSpecific.PlatformSpecificSwitch
import org.example.project.ui.platformSpecific.PlatformSpecificText
import org.example.project.ui.platformSpecific.buttons.PlatformSpecificIconButton
import org.example.project.utils.NetworkError

data class SettingsScreen(
    val client: FirebaseAuthClient,
    val driver: SqlDriver,
    val emailService: EmailService,
    val authentication: Authentication,
    val accountsDataSource: AccountsDataSource,
    val emailDataSource: EmailsDataSource,
    val attachmentsDataSource: AttachmentsDataSource
) : Screen {

    @Composable
    override fun Content() {
        settingsPage(client, driver, authentication)
    }

    @Composable
    fun settingsPage(client: FirebaseAuthClient, driver: SqlDriver, authentication: Any?) {
        val navigator = LocalNavigator.currentOrThrow

//        // db related stuff
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

        this.authentication.amILoggedIn(accountsDataSource)

        val accounts = remember { mutableStateOf(this.authentication.getAccounts(accountsDataSource)) }
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


        FluentThemeEntry {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {

                PlatformSpecificText(text = "Settings page", modifier = Modifier.padding(bottom = 24.dp), fontSize = 32)

                var minimise by remember { mutableStateOf(false) }

                Column {
                    // Add account button
                    Button(onClick = {
                        scope.launch {
                            val re = this@SettingsScreen.authentication.authenticateUser(client, accountsDataSource)
                            r = re.first
                            e = re.second

                            // Update accounts
                            accounts.value = this@SettingsScreen.authentication.getAccounts(accountsDataSource)

                            // Sync will happen automatically due to LaunchedEffect
                        }
                    }) {
                        Text("Add account (GMAIL)")
                    }

                    LazyRow {
                        items(accounts.value) { account ->
                            Text(account.email)
                            // Logout button (in your existing logout logic)
                            Button(onClick = {
                                scope.launch(Dispatchers.IO) {
                                    this@SettingsScreen.authentication.logout(accountsDataSource, account.email)

                                    // Update accounts
                                    withContext(Dispatchers.Main) {
                                        accounts.value =
                                            this@SettingsScreen.authentication.getAccounts(accountsDataSource)

                                        // Sync will happen automatically due to LaunchedEffect
                                    }
                                }
                            }) {
                                Text("Logout")
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                        .border(2.dp, Color.DarkGray, RoundedCornerShape(0.5f))
                ) {
                    Row {
                        PlatformSpecificText(text = "Minimise", modifier = Modifier.padding(bottom = 18.dp))
                        PlatformSpecificSwitch(minimise, { minimise = !minimise })
                    }
                }
            }
        }
    }
}


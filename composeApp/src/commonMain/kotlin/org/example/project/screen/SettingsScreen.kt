package org.example.project.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.adapter.primitive.IntColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.Emails
import com.example.project.database.LuminaDatabase
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.component.*
import com.konyaco.fluent.icons.Icons
import com.konyaco.fluent.icons.filled.Mail
import com.konyaco.fluent.icons.filled.MailDismiss
import com.konyaco.fluent.icons.filled.MailOff
import com.konyaco.fluent.icons.regular.Delete
import kotlinx.coroutines.*
import org.example.project.Authentication
import org.example.project.EmailService
import org.example.project.EmailServiceManager
import org.example.project.networking.FirebaseAuthClient
import org.example.project.networking.OAuthResponse
import org.example.project.shared.data.AccountsDAO
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.AccountsDataSource
import org.example.project.sqldelight.AttachmentsDataSource
import org.example.project.sqldelight.EmailsDataSource
import org.example.project.ui.FluentThemeEntry
import org.example.project.ui.icons.Google
import org.example.project.ui.icons.Microsoft
import org.example.project.ui.platformSpecific.PlatformSpecificCardExpander
import org.example.project.ui.platformSpecific.PlatformSpecificSwitch
import org.example.project.ui.platformSpecific.PlatformSpecificText
import org.example.project.ui.platformSpecific.buttons.PlatformSpecificIconButton
import org.example.project.utils.NetworkError

enum class AuthProvider(val domain: String) {
    GOOGLE("google.com"),
    MICROSOFT("microsoft.com"),
    FACEBOOK("facebook.com"),
    TWITTER("twitter.com"),
    GITHUB("github.com");

    companion object {
        fun fromDomain(domain: String): AuthProvider? =
            values().find { it.domain == domain }
    }
}

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
    fun settingsPage(client: FirebaseAuthClient, driver: SqlDriver, authentication: Authentication) {
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


        FluentTheme {
            Column(modifier = Modifier.padding(16.dp).fillMaxSize(), horizontalAlignment = Alignment.Start) {

                Text(
                    text = "Settings page",
                    modifier = Modifier.padding(bottom = 24.dp),
                    style = FluentTheme.typography.titleLarge
                )

                ConnectedInboxes(
                    accounts = accounts.value,
                    onAddAccount = {
                        println("Add Account Clicked")

                        scope.launch {
                            val re = authentication.authenticateUser(client, accountsDataSource)
                            r = re.first
                            e = re.second

                            // Update accounts
                            accounts.value = authentication.getAccounts(accountsDataSource)

                            // Sync will happen automatically due to LaunchedEffect
                        }
                        // Show account-adding logic
                    },
                    onLogoutAccount = { account ->
                        authentication.logout(accountsDataSource, account.email)
                        accounts.value = accounts.value.filter { it != account }.toMutableList()
                        println("Logged out: ${account.email}")
                    },
                    onLogoutAll = {
                        accounts.value.map { account ->
                            authentication.logout(accountsDataSource, account.email)
                        }

                        accounts.value = mutableListOf()
                        println("Logged out of all accounts")
                    }
                )

            }
        }
    }

    @Composable
    fun ConnectedInboxes(
        accounts: MutableList<AccountsDAO>,
        onAddAccount: () -> Unit,
        onLogoutAccount: (account: AccountsDAO) -> Unit,
        onLogoutAll: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Connected Inboxes", style = FluentTheme.typography.subtitle)

            accounts.forEach { account ->
                AccountRow(
                    account = account,
                    onLogout = { onLogoutAccount(account) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AccentButton(onClick = onAddAccount) {
                    Text("Add Account (Gmail)")
                }
                Button(
                    onClick = onLogoutAll,
                ) {
                    Text("Log Out All")
                }
            }
        }
    }

    @Composable
    fun AccountRow(account: AccountsDAO, onLogout: () -> Unit) {
        println(account.providerId)
        PlatformSpecificCardExpander(
            heading = account.email,
            caption = account.providerId,
            icon = when (account.providerId) {
                AuthProvider.GOOGLE.domain -> Google
                AuthProvider.MICROSOFT.domain -> Microsoft
                else -> Icons.Filled.Mail
            },
            trailing = {
                Button(
                    onClick = onLogout,
                    iconOnly = true
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        imageVector = Icons.Default.Delete,
                        contentDescription = null
                    )
                }
            }
        )
    }
}


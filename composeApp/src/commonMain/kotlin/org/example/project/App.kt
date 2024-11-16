package org.example.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.sqldelight.db.SqlDriver
import com.example.AccountTableQueries
import com.example.EmailTableQueries
import com.example.project.database.LuminaDatabase
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLData
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SettingsListener
import com.russhwolf.settings.observable.makeObservable
import io.ktor.http.ContentType.Text.Html
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import lumina.composeapp.generated.resources.Res
import lumina.composeapp.generated.resources.compose_multiplatform
import org.example.project.shared.AppModule
import org.example.project.sqldelight.AccountDataSource
import org.example.project.sqldelight.EmailDataSource

@OptIn(ExperimentalSettingsApi::class)
@Composable
@Preview
fun App(emailService: EmailService, driver: SqlDriver) {
    MaterialTheme {

        // db related stuff
        val database = LuminaDatabase(driver)
        val accountQueries = database.accountTableQueries

        // Email db stuff
        val emailQueries = database.emailTableQueries
        val emailDataSource: EmailDataSource = EmailDataSource(database)

        val settings = Settings()
        var showContent by remember { mutableStateOf(false) }
        var emailAddress by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var loggedIn by remember { mutableStateOf(false) }

        val observableSettings: ObservableSettings = settings.makeObservable()

        observableSettings.putString("emailAddress", "")
        observableSettings.putString("password", "")
        observableSettings.putBoolean("login", false)

        observableSettings.addBooleanListener("login", defaultValue = false) { value -> loggedIn = value }
        observableSettings.addStringListener("emailAddress", defaultValue = "") { value -> emailAddress = value }
        observableSettings.addStringListener("password", defaultValue = "") { value -> password = value }
//        val state = rememberWebViewState("https://example.com")
//        WebView(state)


        if (!loggedIn) {
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp).verticalScroll(
                    rememberScrollState()
                )) {
                    Text("Login", fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 24.dp))
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
                }
            }
        } else {
            Button(onClick = { logout(observableSettings) }) {
                Text(
                    "Logout"
                )
            }
            displayEmails(emailDataSource, observableSettings,emailQueries, accountQueries, emailService, loggedIn, emailAddress, password)
        }
    }

}

fun login(observableSettings: ObservableSettings, accountQueries: AccountTableQueries, emailAddress: String, password: String): Unit {
    observableSettings.putString("emailAddress", emailAddress)
    observableSettings.putString("password", password)
    observableSettings.putBoolean("login", true)

    val accountExists: String? = accountQueries.selectAccount(emailAddress).executeAsOneOrNull()

    if (accountExists == null) {
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
    emailDataSource: EmailDataSource,
    observableSettings: ObservableSettings,
    emailTableQueries: EmailTableQueries,
    accountQueries: AccountTableQueries,
    emailService: EmailService,
    loggedIn: Boolean,
    emailAddress: String,
    password: String
) {


    if (loggedIn && emailAddress.isNotEmpty() && password.isNotEmpty()) {
        val emailMessages: List<Email> = emailService.getEmails(emailDataSource, emailTableQueries, accountQueries, emailAddress, password)

        Column {
            emailMessages.forEach { email: Email ->
                val state = rememberRichTextState().setHtml(email.body)

                Column(
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = Color.DarkGray,
                        shape = RoundedCornerShape(4.dp)
                    ).background(
                        color = Color.LightGray
                    )
                ) {
                    Text(
                        text = email.from ?: "No from"
                    )
                    Text(
                        text = email.subject ?: "No subject"
                    )
                    RichText(
                        state = state
                    )
                }
            }
        }
//        ksoupHtmlParser.end()

    } else {
        Text(
            text = "Please log in."
        )
    }
}


expect class EmailService {

    fun getEmails(
        emailDataSource: EmailDataSource,
        emailTableQueries: EmailTableQueries,
        accountQueries: AccountTableQueries,
        emailAddress: String,
        password: String
    ): List<Email>
}

data class Email(
    val from: String?,
    val subject: String?,
    val body: String,
    val to: String?,
    val cc: String?,
    val bcc: String?,
    val account: String
)


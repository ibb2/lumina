package org.example.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import lumina.composeapp.generated.resources.Res
import lumina.composeapp.generated.resources.compose_multiplatform

@Composable
@Preview
fun App(emailService: EmailService) {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        var emailAddress by remember { mutableStateOf("email@example.com")}
        var password by remember {mutableStateOf("")}
        var login by remember { mutableStateOf(false) }

        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Column {
                TextField(
                    value = emailAddress,
                    onValueChange = { it -> emailAddress = it},
                    label = { Text("Email Address") }
                )
                TextField(
                    value = password,
                    onValueChange = { it -> password = it},
                    label = { Text("Password") }
                )
                Button(onClick = {
                   login = true
                }) {
                    Text("Login")
                }
            }
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
            displayEmails(emailService, emailAddress, password, login)

        }
    }
}


@Composable
fun displayEmails(emailService: EmailService, emailAddress: String, password: String, login: Boolean) {

    if (login) {
        val emailMessages: Array<Email> = emailService.getEmails(emailAddress, password)

        Column {
            emailMessages.forEach { email: Email ->
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
                        text = email.subject
                    )
                    Text(
                        text = email.body
                    )
                }
            }
        }
    } else {
        Text(
            text = "Please log in."
        )
    }
}


expect class EmailService {
    fun getEmails(emailAddress: String, password: String): Array<Email>
}

data class Email(
    val from: String?,
    val subject: String,
    val body: String
)

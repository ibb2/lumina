package org.example.project.ui.platformSpecific.emails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.background.Mica
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.component.FluentDialog
import com.konyaco.fluent.component.Text
import com.konyaco.fluent.component.TextField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.example.project.EmailService
import org.example.project.data.NewEmail
import org.example.project.sqldelight.EmailsDataSource

@Composable
actual fun sendEmail(
    emailService: EmailService,
    emailDataSource: EmailsDataSource,
    onDismiss: () -> Unit
) {
    var sendEmailFrom by remember { mutableStateOf("") }
    var sendEmailTo by remember { mutableStateOf("") }
    var sendEmailSubject by remember { mutableStateOf("") }
    var sendEmailBody by remember { mutableStateOf("") }

    fun sendEmailAction() {
        CoroutineScope(Dispatchers.IO).launch {
            val sentEmailSuccess =
                emailService.sendNewEmail(
                    emailDataSource,
                    NewEmail(
                        from = sendEmailFrom,
                        to = sendEmailTo,
                        subject = sendEmailSubject,
                        body = sendEmailBody
                    ),
                    sendEmailFrom
                )
            println("Email sent successfully? $sentEmailSuccess")
        }
        onDismiss()
    }

    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    FluentDialog(
        visible = true,
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("From", style = FluentTheme.typography.bodyStrong)
            TextField(
                value = from,
                onValueChange = { from = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Your email address") }
            )

            Text("To", style = FluentTheme.typography.bodyStrong)
            TextField(
                value = to,
                onValueChange = { to = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Recipient email address") }
            )

            Text("Subject", style = FluentTheme.typography.bodyStrong)
            TextField(
                value = subject,
                onValueChange = { subject = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Email subject") }
            )

            Text("Message", style = FluentTheme.typography.bodyStrong)
            TextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text("Write your message here") },
                singleLine = false
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
//                        onSend(from, to, subject, body)
//                        onClose()
                    },
                    disabled = !(from.isNotEmpty() && to.isNotEmpty() && body.isNotEmpty())
                ) {
                    Text("Send")
                }
            }
        }
    }
}

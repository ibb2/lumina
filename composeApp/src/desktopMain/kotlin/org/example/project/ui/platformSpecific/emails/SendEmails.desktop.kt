package org.example.project.ui.platformSpecific.emails

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.component.FluentDialog
import com.konyaco.fluent.component.Text
import com.konyaco.fluent.component.TextField
import org.example.project.EmailService
import org.example.project.sqldelight.EmailsDataSource

@Composable
actual fun sendEmail(
    emailService: EmailService,
    emailDataSource: EmailsDataSource,
    onClose: () -> Unit,
    onSend: (from: String, to: String, subject: String, body: String) -> Unit,
) {

    var from by remember { mutableStateOf(TextFieldValue()) }
    var to by remember { mutableStateOf(TextFieldValue()) }
    var subject by remember { mutableStateOf(TextFieldValue()) }
    var body by remember { mutableStateOf(TextFieldValue()) }

    FluentDialog(
        visible = true,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = from,
                onValueChange = {
                    println("It $it")
                    from = it
                                },
                modifier = Modifier.fillMaxWidth(),
                header = {
                    Text("From", style = FluentTheme.typography.bodyStrong)
                },
                placeholder = { Text("Your email address") }
            )

            TextField(
                value = to,
                onValueChange = { to = it },
                modifier = Modifier.fillMaxWidth(),
                header = {
                    Text("To", style = FluentTheme.typography.bodyStrong)
                },
                placeholder = { Text("Recipient email address") }
            )

            TextField(
                value = subject,
                onValueChange = { subject = it },
                modifier = Modifier.fillMaxWidth(),
                header = {
                    Text("Subject", style = FluentTheme.typography.bodyStrong)
                },
                placeholder = { Text("Email subject") }
            )

            TextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                header = {
                    Text("Message", style = FluentTheme.typography.bodyStrong)
                },
                placeholder = { Text("Write your message here") },
                singleLine = false
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Button(onClick = onClose) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSend(from.text, to.text, subject.text, body.text)
                        onClose()
                    },
                    disabled = !(from.text.isNotEmpty() && to.text.isNotEmpty() && body.text.isNotEmpty())
                ) {
                    Text("Send")
                }
            }
        }
    }
}

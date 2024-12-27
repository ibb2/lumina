package org.example.project.ui.platformSpecific.emails

import androidx.compose.runtime.Composable
import org.example.project.EmailService
import org.example.project.sqldelight.EmailsDataSource

@Composable
expect fun sendEmail(
        emailService: EmailService,
        emailDataSource: EmailsDataSource,
        onDismiss: () -> Unit
)

package org.example.project.ui.platformSpecific.emails

import androidx.compose.runtime.Composable

@Composable
expect fun emailsDialog(visible: Boolean, emailFromUser: String, emailSubject: String, emailContent: String)
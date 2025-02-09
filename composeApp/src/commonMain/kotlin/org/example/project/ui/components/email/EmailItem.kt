package org.example.project.ui.components.email

import Switch_access_shortcut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.db.SqlDriver
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.EmailServiceManager
import org.example.project.deleteEmail
import org.example.project.read
import org.example.project.shared.data.EmailsDAO
import org.example.project.sqldelight.EmailsDataSource
import org.example.project.EmailService
import org.example.project.screen.EmailScreen
import org.example.project.Authentication


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmailItem(
    emails: MutableList<EmailsDAO>,
    email: EmailsDAO,
    index: Number,
    emailAddress: String,
    emailDataSource: EmailsDataSource,
    emailService: EmailService, authentication: Authentication,driver: SqlDriver
) {

    val interactionSource = remember { MutableInteractionSource() }
    val showActions by interactionSource.collectIsHoveredAsState() // State for visibility

    var isRead by remember { mutableStateOf(email.isRead) }

//    val displayEmail = { displayEmailBody(!display, email) }

    val navigator = LocalNavigator.currentOrThrow

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp).hoverable(
                interactionSource = interactionSource
            ).onClick {

                navigator.push(EmailScreen(email, index, emailService, authentication, driver))

            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Subtle elevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top, // Align to top for longer previews
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Column {
                    Text(
                        text = email.senderAddress,
                        style = if (isRead) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            imageVector = Switch_access_shortcut, // Use Rounded, Filled, Outlined, etc.
                            contentDescription = "Indicates email direction: sender to recipient",
                            modifier = Modifier.scale(scaleX = -1f, scaleY = 1f).rotate(230f)
                        )
                        Text(
                            text = emailAddress,
                            style = if (isRead) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(2f)) { // Subject and body preview
                Text(
                    text = if (email.subject.length > 60) {
                        email.subject.substring(0, 50) + "..."
                    } else {
                        email.subject
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (email.body.length > 60) {
                        email.body.substring(0, 50) + "..."
                    } else {
                        email.body
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, // Limit preview lines
                    overflow = TextOverflow.Ellipsis // Add ellipsis if needed
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (showActions) { // Actions revealed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.End // Align to the end
                    ) {
                        IconButton(onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                isRead =
                                    read(
                                        email,
                                        emailDataSource,
                                        emailService,
                                        emailAddress
                                    )
                                        ?: false
                            }
                        }) {
                            Icon(
                                if (isRead) Icons.Outlined.MarkEmailUnread else Icons.Outlined.MarkEmailRead,
                                contentDescription = "Read"
                            )
                        }
                        IconButton(onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                deleteEmail(
                                    email,
                                    emailDataSource,
                                    emailService,
                                    emailAddress
                                )
                                // Remove email on the main thread
                                withContext(Dispatchers.Main) {
                                    emails.remove(email)
                                }
                            }
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                        }
                        // ... other buttons
                    }
                }
            }
        }
    }
////
////                            if (attachments.any { it.emailId === email.id }) {
////                                Row {
////                                    attachments.filter { it.emailId === email.id }.forEach { attachment ->
////                                        Row {
////                                            Text(
////                                                text = attachment.fileName,
////                                                modifier = Modifier.padding(8.dp)
////                                            )
////                                            Text(
////                                                text = attachment.size.toString(),
////                                                modifier = Modifier.padding(8.dp)
////                                            )
////                                            Text(
////                                                text = attachment.mimeType,
////                                                modifier = Modifier.padding(8.dp)
////                                            )
////                                        }
////                                    }
////                                }
////                            }

}
package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.konyaco.fluent.component.Button
import com.konyaco.fluent.component.Icon
import com.konyaco.fluent.icons.Icons
import com.konyaco.fluent.icons.regular.Eye
import com.konyaco.fluent.icons.regular.MailRead
import com.konyaco.fluent.icons.regular.MailUnread

@Composable
actual fun PlatformSpecificMarkAsRead(
    modifier: Modifier,
    isRead: Boolean,
    onClick: () -> Unit,
) {
    Button(modifier = modifier, onClick = onClick) {
        if (isRead)
            Icon(Icons.Default.MailRead, contentDescription = "Email Read") else Icon(
            Icons.Default.MailUnread,
            contentDescription = "Email Unread"
        )
    }
}
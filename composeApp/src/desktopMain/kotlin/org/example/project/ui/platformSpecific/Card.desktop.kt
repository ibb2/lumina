package org.example.project.ui.platformSpecific

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.konyaco.fluent.surface.Card
import org.example.project.shared.data.EmailsDAO
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment

@Composable
actual fun PlatformSpecificCard(modifier: Modifier, displayEmail: () -> Unit, content: @Composable () -> Unit) {

    Card(modifier.clickable {
        displayEmail()
    },) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }
}
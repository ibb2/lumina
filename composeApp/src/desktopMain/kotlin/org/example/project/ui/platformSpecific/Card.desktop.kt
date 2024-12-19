package org.example.project.ui.platformSpecific

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.konyaco.fluent.surface.Card
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment

@Composable
actual fun PlatformSpecificCard(modifier: Modifier, content: @Composable () -> Unit) {

    Card(modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }
}
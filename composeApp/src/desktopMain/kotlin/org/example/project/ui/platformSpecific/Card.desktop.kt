package org.example.project.ui.platformSpecific

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.konyaco.fluent.surface.Card
import org.example.project.shared.data.EmailsDAO
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment

@Composable
actual fun PlatformSpecificCard(modifier: Modifier, displayEmail: () -> Unit, content: @Composable () -> Unit) {

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(modifier.widthIn(max = 700.dp)) {
            Card(modifier.clickable {
                displayEmail()
            }) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    content()
                }
            }
        }
    }
}
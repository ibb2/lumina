package org.example.project.ui.platformSpecific

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.LocalContentColor
import com.konyaco.fluent.background.Mica
import com.konyaco.fluent.surface.Card

@Composable
actual fun PlatformSpecificEmailCard(
    modifier: Modifier,
    displayEmail: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(end = 16.dp)) {
        Row(modifier.widthIn(max = 800.dp)) {
            LocalContentColor provides FluentTheme.colors.text.text.primary
            Card(modifier.clickable {
                displayEmail()
            }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    content()
                }
            }
        }
    }
}
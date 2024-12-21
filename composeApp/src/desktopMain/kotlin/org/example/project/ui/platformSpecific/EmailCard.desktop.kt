package org.example.project.ui.platformSpecific

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Mica(modifier) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(modifier.widthIn(max = 1000.dp)) {
                LocalContentColor provides FluentTheme.colors.text.text.primary
                Card(modifier.clickable {
                    displayEmail()
                }) {
                    Column {
                        Mica(modifier) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                content()
                            }
                        }
                    }
                }
            }
        }
    }
}
package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.konyaco.fluent.FluentTheme

@Composable
expect fun PlatformSpecificText(text: String, modifier: Modifier = Modifier, style: TextStyle = FluentTheme.typography.body, fontSize: Int = 12)
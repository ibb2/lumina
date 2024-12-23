package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
expect fun PlatformSpecificText(text: String, modifier: Modifier? = null, style: androidx.compose.ui.text.TextStyle = TextStyle.Default, fontSize: Int = 12): Unit
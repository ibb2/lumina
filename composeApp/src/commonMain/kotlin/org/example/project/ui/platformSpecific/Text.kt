package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformSpecificText(text: String, modifier: Modifier? = null, fontSize: Int = 12): Unit
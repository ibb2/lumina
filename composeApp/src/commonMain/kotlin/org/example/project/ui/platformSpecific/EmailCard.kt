package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformSpecificEmailCard(modifier: Modifier, displayEmail: () -> Unit, content: @Composable () -> Unit): Unit

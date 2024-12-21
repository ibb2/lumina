package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformSpecificCard(content: @Composable () -> Unit): Unit
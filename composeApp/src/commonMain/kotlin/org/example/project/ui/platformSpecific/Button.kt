package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformSpecificButton(onClick: () -> Unit,content: @Composable () -> Unit)
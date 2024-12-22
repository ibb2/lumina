package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformSpecificSwitch(state: Boolean, changed: () -> Unit)
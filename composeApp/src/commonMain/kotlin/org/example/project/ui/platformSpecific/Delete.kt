package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun PlatformSpecificDelete(modifier: Modifier, onClick: () -> Unit): Unit
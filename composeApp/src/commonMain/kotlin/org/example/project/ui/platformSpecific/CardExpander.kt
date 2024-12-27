package org.example.project.ui.platformSpecific

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
expect fun PlatformSpecificCardExpander(
    heading: String,
    caption: String,
    icon: ImageVector?,
    trailing: @Composable () -> Unit)